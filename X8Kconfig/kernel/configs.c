/*
 * kernel/configs.c
 * Echo the kernel .config file used to build the kernel
 *
 * Copyright (C) 2002 Khalid Aziz <khalid_aziz@hp.com>
 * Copyright (C) 2002 Randy Dunlap <rdunlap@xenotime.net>
 * Copyright (C) 2002 Al Stone <ahs3@fc.hp.com>
 * Copyright (C) 2002 Hewlett-Packard Company
 *
 * Modified by doixanh at xda-developers for extracting current running kernel config
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE, GOOD TITLE or
 * NON INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/proc_fs.h>
#include <linux/seq_file.h>
#include <linux/init.h>
#include <asm/uaccess.h>

/**************************************************/
/* the actual current config file                 */

/*
 * Define kernel_config_data and kernel_config_data_size, which contains the
 * wrapped and compressed configuration file.  The file is first compressed
 * with gzip and then bounded by two eight byte magic numbers to allow
 * extraction from a binary kernel image:
 *
 *   IKCFG_ST
 *   <image>
 *   IKCFG_ED
 */
#define MAGIC_START	"IKCFG_ST"
#define MAGIC_END	"IKCFG_ED"
//#include "config_data.h"		// no we want to extract the original kernel config from the kernel!

#define X8
#define X10M_
#define X10MP_


// patch offsets
#ifdef X8
#define DEVICE_NAME				"X8"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00B0654			// kallsyms_lookup_name
#endif

#ifdef X10M
#define DEVICE_NAME				"X10 mini"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00AF6D8			// kallsyms_lookup_name
#endif

#ifdef X10MP
#define DEVICE_NAME				"X10 mini pro"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00B09F0			// kallsyms_lookup_name
#endif


// dx: our external variables/functions
typedef unsigned long (*kallsyms_lookup_name_type)(const char *name);
static kallsyms_lookup_name_type kallsyms_lookup_name_dx;

static void * kernel_config_data_dx;

#define MAGIC_SIZE (sizeof(MAGIC_START) - 1)

// in kallsyms look for kernel_config_data and the one RIGHT AFTER kernel_config_data
#ifdef X8
#define kernel_config_data_size (0xc0395f44 - 0xc039340c)
#endif

#ifdef X10M
// can someone with x10mini look in kallsyms and fill this field?
#define kernel_config_data_size ()
#endif

#ifdef X10MP
// can someone with x10minipro look in kallsyms and fill this field?
#define kernel_config_data_size ()
#endif

#ifndef CONFIG_IKCONFIG_PROC
#define CONFIG_IKCONFIG_PROC
#endif

#ifdef CONFIG_IKCONFIG_PROC

static ssize_t
ikconfig_read_current(struct file *file, char __user *buf,
		      size_t len, loff_t * offset)
{
	return simple_read_from_buffer(buf, len, offset,
				       kernel_config_data_dx + MAGIC_SIZE,
				       kernel_config_data_size);
}

static const struct file_operations ikconfig_file_ops = {
	.owner = THIS_MODULE,
	.read = ikconfig_read_current,
};

static int __init ikconfig_init(void)
{
	struct proc_dir_entry *entry;
	
	// our 'GetProcAddress' :D
	kallsyms_lookup_name_dx = (void*) OFS_KALLSYMS_LOOKUP_NAME;

	// get original kernel_config_data
	kernel_config_data_dx = (void*) kallsyms_lookup_name_dx("kernel_config_data");
	
	printk("X8Kconfig: v001. module loaded. Build target: " DEVICE_NAME);
	printk("X8Kconfig: config.gz start at %X,size %d", (int) kernel_config_data_dx, kernel_config_data_size);
	
	/* create the current config file */
	entry = proc_create("config.gz", S_IFREG | S_IRUGO, NULL,
			    &ikconfig_file_ops);
	if (!entry)
		return -ENOMEM;

	printk("X8Kconfig: procfs entry created at /proc/config.gz");
	entry->size = kernel_config_data_size;

	return 0;
}

static void __exit ikconfig_cleanup(void)
{
	remove_proc_entry("config.gz", NULL);
}

module_init(ikconfig_init);
module_exit(ikconfig_cleanup);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Randy Dunlap");
MODULE_DESCRIPTION("Echo the kernel .config file used to build the kernel");

#endif /* CONFIG_IKCONFIG_PROC */
