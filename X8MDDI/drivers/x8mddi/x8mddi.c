/* 
 * Author: doixanh at xda-developers
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2, as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

#include <linux/module.h>
#include <linux/kernel.h>	

// dx: fix mddi driver
#define X8
#define X10M_
#define X10MP_
#define DX_MODULE_NAME			"x8mddi"
#define DX_MODULE_VER			"v001"


// patch offsets
#ifdef X8
#define DEVICE_NAME				"X8"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00B0654			// kallsyms_lookup_name
#endif

#ifdef X10M
#define DEVICE_NAME				"X10 mini"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00AF6D8			// kallsyms_lookup_name
#define	OFS_ESD_RECOVERY_SAMSUNG	0xC01A2A8C			// esd_recovery_func for samsung mddi
#define	OFS_ESD_RECOVERY_HITACHI	0xC01A40B8			// esd_recovery_func for hitachi mddi
#endif

#ifdef X10MP
#define DEVICE_NAME				"X10 mini pro"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00B09F0			// kallsyms_lookup_name
#define	OFS_ESD_RECOVERY_SAMSUNG	0xC01C0288			// esd_recovery_func for samsung mddi
#define	OFS_ESD_RECOVERY_HITACHI	0xC01BEC6C			// esd_recovery_func for hitachi mddi
#endif


// dx: our external variables/functions
typedef unsigned long (*kallsyms_lookup_name_type)(const char *name);
static kallsyms_lookup_name_type kallsyms_lookup_name_dx;

static void esd_recovery_func(void * nothing)
{
	// we do nothing here. no need recovery, it's laggy!
	return;
}

// inline memory patch an unsigned integer
static void patch(unsigned int addr, unsigned int value) {
	*(unsigned int*)addr = value;
}

// patch to an jump obcode
static void patch_to_jmp(unsigned int addr, void * func) {
	int write_value;
	// calculate the offset
	write_value = ((((unsigned int)func - 8 - addr) >> 2) & 0x00FFFFFF);
	// add the unconditional jump opcode
	write_value |= 0xEA000000;
	// and patch it
	patch(addr, write_value);
}

// dx: our modified module init
static int __init x8screen_init(void)
{
#ifdef X8
	int esd_recovery_func_org;
#endif

	// our 'GetProcAddress' :D
	kallsyms_lookup_name_dx = (void*) OFS_KALLSYMS_LOOKUP_NAME;
	
	printk(KERN_INFO DX_MODULE_NAME": module " DX_MODULE_VER " loaded. Build target : " DEVICE_NAME "\n");

#ifndef X8
	// x10 mini / pro has two different screen: samsung/hitachi. We have to patch both

	// hijack calls to samsung mddi esd_recovery_func
	patch_to_jmp(OFS_ESD_RECOVERY_SAMSUNG, &esd_recovery_func);

	// hijack calls to hitachi mddi esd_recovery_func
	patch_to_jmp(OFS_ESD_RECOVERY_HITACHI, &esd_recovery_func);
	
#else	// x8
	// get original recovery function
	esd_recovery_func_org = (int) kallsyms_lookup_name_dx("esd_recovery_func");
	
	// hijack calls only
	patch_to_jmp(esd_recovery_func_org, &esd_recovery_func);
#endif

	printk(KERN_INFO DX_MODULE_NAME": patching done. enjoy no lag ;)\n");
	return 0;
}

MODULE_LICENSE("GPL");
MODULE_AUTHOR("doixanh at xda-developers");
MODULE_DESCRIPTION("Fixes for laggy X8/X10m/X10mp MDDI drivers");

module_init(x8screen_init);
