/* 
 * Author: doixanh at xda-developers
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2, as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */
#ifndef CONFIG_KEXEC
#define CONFIG_KEXEC
#endif

#include <linux/module.h>
#include <linux/kernel.h>	
#include <linux/mm.h>
#include <linux/kexec.h>
#include <linux/delay.h>
#include <linux/reboot.h>
#include <linux/io.h>
#include <asm/pgtable.h>
#include <asm/pgalloc.h>
#include <asm/mmu_context.h>
#include <asm/cacheflush.h>
#include <asm/mach-types.h>
#include <asm/setup.h>
#include "../mach-msm/smd_private.h"

// dx: device and module info
#define X8
#define X10M_
#define X10MP_
#define DX_MODULE_NAME			"x8kexec"
#define DX_MODULE_VER			"alpha1"


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

#define define_func(type, name, params)		typedef type (*name##_def)(params); \
						static name##_def name##_dx;
#define lookup_func(name)			name##_dx = (void*) kallsyms_lookup_name_dx(#name)
#define define_var(type, name)			static type * name##_dx;
#define lookup_var(name)			name##_dx = (typeof(name##_dx)) kallsyms_lookup_name_dx(#name)

// dx: our external variables/functions
define_func(unsigned long, kallsyms_lookup_name, const char *name)
define_func(void, machine_kexec, struct kimage *image)
define_func(void, setup_mm_for_kdump, char mode)
define_func(void, append_crash_params_cmdline, void)
define_func(void, cpu_v6_reset, unsigned int addr)
define_func(void, cpu_v6_proc_fin, void)

define_var(unsigned long, kexec_start_address)
define_var(unsigned long, kexec_indirection_page)
define_var(unsigned long, kexec_mach_type)
define_var(unsigned long, kexec_boot_atags)
define_var(unsigned char, relocate_new_kernel)
define_var(unsigned int, relocate_new_kernel_size)


void machine_kexec_custom(struct kimage *image)
{
	unsigned long page_list;
	unsigned long reboot_code_buffer_phys;
	void *reboot_code_buffer;


	page_list = image->head & PAGE_MASK;

	/* we need both effective and real address here */
	reboot_code_buffer_phys =
	    page_to_pfn(image->control_code_page) << PAGE_SHIFT;
	reboot_code_buffer = (void*) page_address(image->control_code_page);

	/* Prepare parameters for reboot_code_buffer*/
	*kexec_start_address_dx = image->start;
	*kexec_indirection_page_dx = page_list;
	*kexec_mach_type_dx = machine_arch_type;
	*kexec_boot_atags_dx = image->start - KEXEC_ARM_ZIMAGE_OFFSET + KEXEC_ARM_ATAGS_OFFSET;
	
	printk(KERN_INFO "start=%8lx, inpage=%8lx, machtype=%8lx, atag=%8lx\n", 
		*kexec_start_address_dx, *kexec_indirection_page_dx,
		*kexec_mach_type_dx, *kexec_boot_atags_dx);
	

	/* copy our kernel relocation code to the control code page */
	memcpy(reboot_code_buffer,
	       relocate_new_kernel_dx, *relocate_new_kernel_size_dx);


	flush_icache_range((unsigned long) reboot_code_buffer,
			   (unsigned long) reboot_code_buffer + KEXEC_CONTROL_PAGE_SIZE);
	printk(KERN_INFO "Bye!\n");

	cpu_v6_proc_fin_dx();
	printk(KERN_INFO "after cpu_proc_fin\n");
	setup_mm_for_kdump_dx(0); /* mode is not used, so just pass 0*/
	printk(KERN_INFO "after setup_mm_for_kdump going to cpu reset\n");
	/*append_crash_params_cmdline_dx();
	printk(KERN_INFO "after append_crash_params\n");*/
	cpu_v6_reset_dx(reboot_code_buffer_phys);
}

// inline memory patch an unsigned integer
static void patch(unsigned int addr, unsigned int value) {
	*(unsigned int*)addr = value;
}

// patch to a jump opcode
static void patch_to_jmp(unsigned int addr, void * func) {
	int write_value;
	// calculate the offset
	write_value = ((((unsigned int)func - 8 - addr) >> 2) & 0x00FFFFFF);
	// add the unconditional jump opcode
	write_value |= 0xEA000000;
	// and patch it
	patch(addr, write_value);
}

// dx: our module init
static int __init x8kexec_init(void)
{
	// our 'GetProcAddress' :D
	kallsyms_lookup_name_dx = (void*) OFS_KALLSYMS_LOOKUP_NAME;
	
	printk(KERN_INFO DX_MODULE_NAME": module " DX_MODULE_VER " loaded. Build target : " DEVICE_NAME "\n");

#ifdef X8
	// get original vars/functions
	//lookup_func(machine_kexec);
	lookup_func(machine_kexec);
	lookup_func(setup_mm_for_kdump);
	lookup_func(append_crash_params_cmdline);
	lookup_func(cpu_v6_reset);
	lookup_func(cpu_v6_proc_fin);
	
	lookup_var(relocate_new_kernel);
	lookup_var(relocate_new_kernel_size);
	lookup_var(kexec_start_address);
	lookup_var(kexec_indirection_page);
	lookup_var(kexec_mach_type);
	lookup_var(kexec_boot_atags);

	printk(KERN_INFO DX_MODULE_NAME": machine_kexec at %X\n", (unsigned int) machine_kexec_dx);
	patch_to_jmp((unsigned int) machine_kexec_dx, &machine_kexec_custom);
#else
	// x10 mini / pro
#endif
	printk(KERN_INFO DX_MODULE_NAME": patching done.\n");
	return 0;
	
}

MODULE_LICENSE("GPL");
MODULE_AUTHOR("doixanh at xda-developers");
MODULE_DESCRIPTION("Fixes for X8/X10m/X10mp kexec");

module_init(x8kexec_init);
