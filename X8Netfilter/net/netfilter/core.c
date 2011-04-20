/* netfilter.c: look after the filters for various protocols.
 * Heavily influenced by the old firewall.c by David Bonn and Alan Cox.
 *
 * Thanks to Rob `CmdrTaco' Malda for not influencing this code in any
 * way.
 *
 * Rusty Russell (C)2000 -- This code is GPL.
 */
#include <linux/kernel.h>
#include <linux/netfilter.h>
#include <net/protocol.h>
#include <linux/init.h>
#include <linux/skbuff.h>
#include <linux/wait.h>
#include <linux/module.h>
#include <linux/interrupt.h>
#include <linux/if.h>
#include <linux/netdevice.h>
#include <linux/inetdevice.h>
#include <linux/proc_fs.h>
#include <linux/mutex.h>
#include <net/net_namespace.h>
#include <net/sock.h>
#include <linux/if.h>
#include "nf_internals.h"


// dx: more includes
#include <linux/mroute.h>
#include <linux/igmp.h>
#include <net/ip.h>
#include <net/route.h>
#include <net/xfrm.h>
#include <linux/pkt_sched.h>	
#include <net/tcp.h>

static DEFINE_MUTEX(afinfo_mutex);

// dx : defines
#define DX_MODULE_NAME 			"x8netfilter"

#define IP_CMSG_PKTINFO		1
#define IP_CMSG_TTL		2
#define IP_CMSG_TOS		4
#define IP_CMSG_RECVOPTS	8
#define IP_CMSG_RETOPTS		16
#define IP_CMSG_PASSSEC		32
#define IP_CMSG_ORIGDSTADDR     64

#define DXDBG(x)					
#define X8
#define X10Ma
#define X10MPa

// patch offsets
#ifdef X8
#define DEVICE_NAME			"X8"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00B0654			// kallsyms_lookup_name
#endif

#ifdef X10M
#define DEVICE_NAME			"X10 mini"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00AF6D8			// kallsyms_lookup_name
#endif

#ifdef X10MP
#define DEVICE_NAME			"X10 mini pro"
#define OFS_KALLSYMS_LOOKUP_NAME	0xC00B09F0			// kallsyms_lookup_name
#endif

#define ECN_OR_COST(class)	TC_PRIO_##class
const __u8 ip_tos2prio[16] = {
	TC_PRIO_BESTEFFORT,
	ECN_OR_COST(FILLER),
	TC_PRIO_BESTEFFORT,
	ECN_OR_COST(BESTEFFORT),
	TC_PRIO_BULK,
	ECN_OR_COST(BULK),
	TC_PRIO_BULK,
	ECN_OR_COST(BULK),
	TC_PRIO_INTERACTIVE,
	ECN_OR_COST(INTERACTIVE),
	TC_PRIO_INTERACTIVE,
	ECN_OR_COST(INTERACTIVE),
	TC_PRIO_INTERACTIVE_BULK,
	ECN_OR_COST(INTERACTIVE_BULK),
	TC_PRIO_INTERACTIVE_BULK,
	ECN_OR_COST(INTERACTIVE_BULK)
};


// external variables / functions
static int (*sysctl_ip_default_ttl_dx);
static int (*sysctl_igmp_max_msf_dx);
//static __u8 ip_tos2prio_dx[16];
typedef unsigned long (*kallsyms_lookup_name_type)(const char *name);
static kallsyms_lookup_name_type kallsyms_lookup_name_dx;
typedef int (*ip_mc_msfget_type)(struct sock *sk, struct ip_msfilter *msf, struct ip_msfilter __user *optval, int __user *optlen);
static ip_mc_msfget_type ip_mc_msfget_dx;
typedef void (*ip_options_undo_type)(struct ip_options * opt);
static ip_options_undo_type ip_options_undo_dx;
typedef int (*ip_mc_gsfget_type)(struct sock *sk, struct group_filter *gsf, struct group_filter __user *optval, int __user *optlen);
static ip_mc_gsfget_type ip_mc_gsfget_dx;
typedef int (*ip_mc_source_type)(int add, int omode, struct sock *sk, struct ip_mreq_source *mreqs, int ifindex);
static ip_mc_source_type ip_mc_source_dx;
typedef int (*ip_options_get_from_user_type)(struct net *net, struct ip_options **optp, unsigned char __user *data, int optlen);
static ip_options_get_from_user_type ip_options_get_from_user_dx;
typedef int (*ip_ra_control_type)(struct sock *sk, unsigned char on, void (*destructor)(struct sock *));
static ip_ra_control_type ip_ra_control_dx;
typedef int (*ip_mc_leave_group_type)(struct sock *sk, struct ip_mreqn *imr);
static ip_mc_leave_group_type ip_mc_leave_group_dx;
typedef int (*ip_mc_msfilter_type)(struct sock *sk, struct ip_msfilter *msf, int ifindex);
static ip_mc_msfilter_type ip_mc_msfilter_dx;
typedef int (*tcp_connect_type)(struct sock *sk);
static tcp_connect_type tcp_connect_dx;
typedef __u32 (*secure_tcp_sequence_number_type)(__be32 saddr, __be32 daddr, __be16 sport, __be16 dport);
static secure_tcp_sequence_number_type secure_tcp_sequence_number_dx;


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

// dx : stock func
static int do_ip_setsockopt_dx(struct sock *sk, int level,
			    int optname, char __user *optval, int optlen)
{
	struct inet_sock *inet = inet_sk(sk);
	int val = 0, err;

	if (((1<<optname) & ((1<<IP_PKTINFO) | (1<<IP_RECVTTL) |
			     (1<<IP_RECVOPTS) | (1<<IP_RECVTOS) |
			     (1<<IP_RETOPTS) | (1<<IP_TOS) |
			     (1<<IP_TTL) | (1<<IP_HDRINCL) |
			     (1<<IP_MTU_DISCOVER) | (1<<IP_RECVERR) |
			     (1<<IP_ROUTER_ALERT) | (1<<IP_FREEBIND) |
			     (1<<IP_PASSSEC) | (1<<IP_TRANSPARENT))) ||
	    optname == IP_MULTICAST_TTL ||
	    optname == IP_MULTICAST_LOOP ||
	    optname == IP_RECVORIGDSTADDR) {
		if (optlen >= sizeof(int)) {
			if (get_user(val, (int __user *) optval))
				return -EFAULT;
		} else if (optlen >= sizeof(char)) {
			unsigned char ucval;

			if (get_user(ucval, (unsigned char __user *) optval))
				return -EFAULT;
			val = (int) ucval;
		}
	}

	/* If optlen==0, it is equivalent to val == 0 */

	if (ip_mroute_opt(optname))
		return ip_mroute_setsockopt(sk, optname, optval, optlen);

	err = 0;
	lock_sock(sk);

	switch (optname) {
	case IP_OPTIONS:
	{
		struct ip_options * opt = NULL;
		if (optlen > 40 || optlen < 0)
			goto e_inval;
		err = ip_options_get_from_user_dx(sock_net(sk), &opt,
					       optval, optlen);
		if (err)
			break;
		if (inet->is_icsk) {
			struct inet_connection_sock *icsk = inet_csk(sk);
				if (inet->opt)
					icsk->icsk_ext_hdr_len -= inet->opt->optlen;
				if (opt)
					icsk->icsk_ext_hdr_len += opt->optlen;
				icsk->icsk_sync_mss(sk, icsk->icsk_pmtu_cookie);
		}
		opt = xchg(&inet->opt, opt);
		kfree(opt);
		break;
	}
	case IP_PKTINFO:
		if (val)
			inet->cmsg_flags |= IP_CMSG_PKTINFO;
		else
			inet->cmsg_flags &= ~IP_CMSG_PKTINFO;
		break;
	case IP_RECVTTL:
		if (val)
			inet->cmsg_flags |=  IP_CMSG_TTL;
		else
			inet->cmsg_flags &= ~IP_CMSG_TTL;
		break;
	case IP_RECVTOS:
		if (val)
			inet->cmsg_flags |=  IP_CMSG_TOS;
		else
			inet->cmsg_flags &= ~IP_CMSG_TOS;
		break;
	case IP_RECVOPTS:
		if (val)
			inet->cmsg_flags |=  IP_CMSG_RECVOPTS;
		else
			inet->cmsg_flags &= ~IP_CMSG_RECVOPTS;
		break;
	case IP_RETOPTS:
		if (val)
			inet->cmsg_flags |= IP_CMSG_RETOPTS;
		else
			inet->cmsg_flags &= ~IP_CMSG_RETOPTS;
		break;
	case IP_PASSSEC:
		if (val)
			inet->cmsg_flags |= IP_CMSG_PASSSEC;
		else
			inet->cmsg_flags &= ~IP_CMSG_PASSSEC;
		break;
	case IP_RECVORIGDSTADDR:
		if (val)
			inet->cmsg_flags |= IP_CMSG_ORIGDSTADDR;
		else
			inet->cmsg_flags &= ~IP_CMSG_ORIGDSTADDR;
		break;
	case IP_TOS:	/* This sets both TOS and Precedence */
		if (sk->sk_type == SOCK_STREAM) {
			val &= ~3;
			val |= inet->tos & 3;
		}
		if (inet->tos != val) {
			inet->tos = val;
			sk->sk_priority = rt_tos2priority(val);
			sk_dst_reset(sk);
		}
		break;
	case IP_TTL:
		if (optlen<1)
			goto e_inval;
		if (val != -1 && (val < 1 || val>255))
			goto e_inval;
		inet->uc_ttl = val;
		break;
	case IP_HDRINCL:
		if (sk->sk_type != SOCK_RAW) {
			err = -ENOPROTOOPT;
			break;
		}
		inet->hdrincl = val ? 1 : 0;
		break;
	case IP_MTU_DISCOVER:
		if (val<0 || val>3)
			goto e_inval;
		inet->pmtudisc = val;
		break;
	case IP_RECVERR:
		inet->recverr = !!val;
		if (!val)
			skb_queue_purge(&sk->sk_error_queue);
		break;
	case IP_MULTICAST_TTL:
		if (sk->sk_type == SOCK_STREAM)
			goto e_inval;
		if (optlen<1)
			goto e_inval;
		if (val == -1)
			val = 1;
		if (val < 0 || val > 255)
			goto e_inval;
		inet->mc_ttl = val;
		break;
	case IP_MULTICAST_LOOP:
		if (optlen<1)
			goto e_inval;
		inet->mc_loop = !!val;
		break;
	case IP_MULTICAST_IF:
	{
		struct ip_mreqn mreq;
		struct net_device *dev = NULL;

		if (sk->sk_type == SOCK_STREAM)
			goto e_inval;
		/*
		 *	Check the arguments are allowable
		 */

		err = -EFAULT;
		if (optlen >= sizeof(struct ip_mreqn)) {
			if (copy_from_user(&mreq, optval, sizeof(mreq)))
				break;
		} else {
			memset(&mreq, 0, sizeof(mreq));
			if (optlen >= sizeof(struct in_addr) &&
			    copy_from_user(&mreq.imr_address, optval, sizeof(struct in_addr)))
				break;
		}

		if (!mreq.imr_ifindex) {
			if (mreq.imr_address.s_addr == htonl(INADDR_ANY)) {
				inet->mc_index = 0;
				inet->mc_addr  = 0;
				err = 0;
				break;
			}
			dev = ip_dev_find(sock_net(sk), mreq.imr_address.s_addr);
			if (dev) {
				mreq.imr_ifindex = dev->ifindex;
				dev_put(dev);
			}
		} else
			dev = __dev_get_by_index(sock_net(sk), mreq.imr_ifindex);


		err = -EADDRNOTAVAIL;
		if (!dev)
			break;

		err = -EINVAL;
		if (sk->sk_bound_dev_if &&
		    mreq.imr_ifindex != sk->sk_bound_dev_if)
			break;

		inet->mc_index = mreq.imr_ifindex;
		inet->mc_addr  = mreq.imr_address.s_addr;
		err = 0;
		break;
	}

	case IP_ADD_MEMBERSHIP:
	case IP_DROP_MEMBERSHIP:
	{
		struct ip_mreqn mreq;

		err = -EPROTO;
		if (inet_sk(sk)->is_icsk)
			break;

		if (optlen < sizeof(struct ip_mreq))
			goto e_inval;
		err = -EFAULT;
		if (optlen >= sizeof(struct ip_mreqn)) {
			if (copy_from_user(&mreq, optval, sizeof(mreq)))
				break;
		} else {
			memset(&mreq, 0, sizeof(mreq));
			if (copy_from_user(&mreq, optval, sizeof(struct ip_mreq)))
				break;
		}

		if (optname == IP_ADD_MEMBERSHIP)
			err = ip_mc_join_group(sk, &mreq);
		else
			err = ip_mc_leave_group_dx(sk, &mreq);
		break;
	}
	case IP_MSFILTER:
	{
		struct ip_msfilter *msf;

		if (optlen < IP_MSFILTER_SIZE(0))
			goto e_inval;
		if (optlen > sysctl_optmem_max) {
			err = -ENOBUFS;
			break;
		}
		msf = kmalloc(optlen, GFP_KERNEL);
		if (!msf) {
			err = -ENOBUFS;
			break;
		}
		err = -EFAULT;
		if (copy_from_user(msf, optval, optlen)) {
			kfree(msf);
			break;
		}
		/* numsrc >= (1G-4) overflow in 32 bits */
		if (msf->imsf_numsrc >= 0x3ffffffcU ||
		    msf->imsf_numsrc > (*sysctl_igmp_max_msf_dx)) {
			kfree(msf);
			err = -ENOBUFS;
			break;
		}
		if (IP_MSFILTER_SIZE(msf->imsf_numsrc) > optlen) {
			kfree(msf);
			err = -EINVAL;
			break;
		}
		err = ip_mc_msfilter_dx(sk, msf, 0);
		kfree(msf);
		break;
	}
	case IP_BLOCK_SOURCE:
	case IP_UNBLOCK_SOURCE:
	case IP_ADD_SOURCE_MEMBERSHIP:
	case IP_DROP_SOURCE_MEMBERSHIP:
	{
		struct ip_mreq_source mreqs;
		int omode, add;

		if (optlen != sizeof(struct ip_mreq_source))
			goto e_inval;
		if (copy_from_user(&mreqs, optval, sizeof(mreqs))) {
			err = -EFAULT;
			break;
		}
		if (optname == IP_BLOCK_SOURCE) {
			omode = MCAST_EXCLUDE;
			add = 1;
		} else if (optname == IP_UNBLOCK_SOURCE) {
			omode = MCAST_EXCLUDE;
			add = 0;
		} else if (optname == IP_ADD_SOURCE_MEMBERSHIP) {
			struct ip_mreqn mreq;

			mreq.imr_multiaddr.s_addr = mreqs.imr_multiaddr;
			mreq.imr_address.s_addr = mreqs.imr_interface;
			mreq.imr_ifindex = 0;
			err = ip_mc_join_group(sk, &mreq);
			if (err && err != -EADDRINUSE)
				break;
			omode = MCAST_INCLUDE;
			add = 1;
		} else /* IP_DROP_SOURCE_MEMBERSHIP */ {
			omode = MCAST_INCLUDE;
			add = 0;
		}
		err = ip_mc_source_dx(add, omode, sk, &mreqs, 0);
		break;
	}
	case MCAST_JOIN_GROUP:
	case MCAST_LEAVE_GROUP:
	{
		struct group_req greq;
		struct sockaddr_in *psin;
		struct ip_mreqn mreq;

		if (optlen < sizeof(struct group_req))
			goto e_inval;
		err = -EFAULT;
		if (copy_from_user(&greq, optval, sizeof(greq)))
			break;
		psin = (struct sockaddr_in *)&greq.gr_group;
		if (psin->sin_family != AF_INET)
			goto e_inval;
		memset(&mreq, 0, sizeof(mreq));
		mreq.imr_multiaddr = psin->sin_addr;
		mreq.imr_ifindex = greq.gr_interface;

		if (optname == MCAST_JOIN_GROUP)
			err = ip_mc_join_group(sk, &mreq);
		else
			err = ip_mc_leave_group_dx(sk, &mreq);
		break;
	}
	case MCAST_JOIN_SOURCE_GROUP:
	case MCAST_LEAVE_SOURCE_GROUP:
	case MCAST_BLOCK_SOURCE:
	case MCAST_UNBLOCK_SOURCE:
	{
		struct group_source_req greqs;
		struct ip_mreq_source mreqs;
		struct sockaddr_in *psin;
		int omode, add;

		if (optlen != sizeof(struct group_source_req))
			goto e_inval;
		if (copy_from_user(&greqs, optval, sizeof(greqs))) {
			err = -EFAULT;
			break;
		}
		if (greqs.gsr_group.ss_family != AF_INET ||
		    greqs.gsr_source.ss_family != AF_INET) {
			err = -EADDRNOTAVAIL;
			break;
		}
		psin = (struct sockaddr_in *)&greqs.gsr_group;
		mreqs.imr_multiaddr = psin->sin_addr.s_addr;
		psin = (struct sockaddr_in *)&greqs.gsr_source;
		mreqs.imr_sourceaddr = psin->sin_addr.s_addr;
		mreqs.imr_interface = 0; /* use index for mc_source */

		if (optname == MCAST_BLOCK_SOURCE) {
			omode = MCAST_EXCLUDE;
			add = 1;
		} else if (optname == MCAST_UNBLOCK_SOURCE) {
			omode = MCAST_EXCLUDE;
			add = 0;
		} else if (optname == MCAST_JOIN_SOURCE_GROUP) {
			struct ip_mreqn mreq;

			psin = (struct sockaddr_in *)&greqs.gsr_group;
			mreq.imr_multiaddr = psin->sin_addr;
			mreq.imr_address.s_addr = 0;
			mreq.imr_ifindex = greqs.gsr_interface;
			err = ip_mc_join_group(sk, &mreq);
			if (err && err != -EADDRINUSE)
				break;
			greqs.gsr_interface = mreq.imr_ifindex;
			omode = MCAST_INCLUDE;
			add = 1;
		} else /* MCAST_LEAVE_SOURCE_GROUP */ {
			omode = MCAST_INCLUDE;
			add = 0;
		}
		err = ip_mc_source_dx(add, omode, sk, &mreqs,
				   greqs.gsr_interface);
		break;
	}
	case MCAST_MSFILTER:
	{
		struct sockaddr_in *psin;
		struct ip_msfilter *msf = NULL;
		struct group_filter *gsf = NULL;
		int msize, i, ifindex;

		if (optlen < GROUP_FILTER_SIZE(0))
			goto e_inval;
		if (optlen > sysctl_optmem_max) {
			err = -ENOBUFS;
			break;
		}
		gsf = kmalloc(optlen, GFP_KERNEL);
		if (!gsf) {
			err = -ENOBUFS;
			break;
		}
		err = -EFAULT;
		if (copy_from_user(gsf, optval, optlen)) {
			goto mc_msf_out;
		}
		/* numsrc >= (4G-140)/128 overflow in 32 bits */
		if (gsf->gf_numsrc >= 0x1ffffff ||
		    gsf->gf_numsrc > (*sysctl_igmp_max_msf_dx)) {
			err = -ENOBUFS;
			goto mc_msf_out;
		}
		if (GROUP_FILTER_SIZE(gsf->gf_numsrc) > optlen) {
			err = -EINVAL;
			goto mc_msf_out;
		}
		msize = IP_MSFILTER_SIZE(gsf->gf_numsrc);
		msf = kmalloc(msize, GFP_KERNEL);
		if (!msf) {
			err = -ENOBUFS;
			goto mc_msf_out;
		}
		ifindex = gsf->gf_interface;
		psin = (struct sockaddr_in *)&gsf->gf_group;
		if (psin->sin_family != AF_INET) {
			err = -EADDRNOTAVAIL;
			goto mc_msf_out;
		}
		msf->imsf_multiaddr = psin->sin_addr.s_addr;
		msf->imsf_interface = 0;
		msf->imsf_fmode = gsf->gf_fmode;
		msf->imsf_numsrc = gsf->gf_numsrc;
		err = -EADDRNOTAVAIL;
		for (i=0; i<gsf->gf_numsrc; ++i) {
			psin = (struct sockaddr_in *)&gsf->gf_slist[i];

			if (psin->sin_family != AF_INET)
				goto mc_msf_out;
			msf->imsf_slist[i] = psin->sin_addr.s_addr;
		}
		kfree(gsf);
		gsf = NULL;

		err = ip_mc_msfilter_dx(sk, msf, ifindex);
	mc_msf_out:
		kfree(msf);
		kfree(gsf);
		break;
	}
	case IP_ROUTER_ALERT:
		err = ip_ra_control_dx(sk, val ? 1 : 0, NULL);
		break;

	case IP_FREEBIND:
		if (optlen<1)
			goto e_inval;
		inet->freebind = !!val;
		break;

	case IP_IPSEC_POLICY:
	case IP_XFRM_POLICY:
		err = -EPERM;
		if (!capable(CAP_NET_ADMIN))
			break;
		err = xfrm_user_policy(sk, optname, optval, optlen);
		break;

	case IP_TRANSPARENT:
		if (!capable(CAP_NET_ADMIN)) {
			err = -EPERM;
			break;
		}
		if (optlen < 1)
			goto e_inval;
		inet->transparent = !!val;
		break;

	default:
		err = -ENOPROTOOPT;
		break;
	}
	release_sock(sk);
	return err;

e_inval:
	release_sock(sk);
	return -EINVAL;
}


static int do_ip_getsockopt_dx(struct sock *sk, int level, int optname,
			    char __user *optval, int __user *optlen)
{
	struct inet_sock *inet = inet_sk(sk);
	int val;
	int len;

	if (level != SOL_IP)
		return -EOPNOTSUPP;

	if (ip_mroute_opt(optname))
		return ip_mroute_getsockopt(sk, optname, optval, optlen);

	if (get_user(len, optlen))
		return -EFAULT;
	if (len < 0)
		return -EINVAL;

	lock_sock(sk);

	switch (optname) {
	case IP_OPTIONS:
	{
		unsigned char optbuf[sizeof(struct ip_options)+40];
		struct ip_options * opt = (struct ip_options *)optbuf;
		opt->optlen = 0;
		if (inet->opt)
			memcpy(optbuf, inet->opt,
			       sizeof(struct ip_options)+
			       inet->opt->optlen);
		release_sock(sk);

		if (opt->optlen == 0)
			return put_user(0, optlen);

		ip_options_undo_dx(opt);

		len = min_t(unsigned int, len, opt->optlen);
		if (put_user(len, optlen))
			return -EFAULT;
		if (copy_to_user(optval, opt->__data, len))
			return -EFAULT;
		return 0;
	}
	case IP_PKTINFO:
		val = (inet->cmsg_flags & IP_CMSG_PKTINFO) != 0;
		break;
	case IP_RECVTTL:
		val = (inet->cmsg_flags & IP_CMSG_TTL) != 0;
		break;
	case IP_RECVTOS:
		val = (inet->cmsg_flags & IP_CMSG_TOS) != 0;
		break;
	case IP_RECVOPTS:
		val = (inet->cmsg_flags & IP_CMSG_RECVOPTS) != 0;
		break;
	case IP_RETOPTS:
		val = (inet->cmsg_flags & IP_CMSG_RETOPTS) != 0;
		break;
	case IP_PASSSEC:
		val = (inet->cmsg_flags & IP_CMSG_PASSSEC) != 0;
		break;
	case IP_RECVORIGDSTADDR:
		val = (inet->cmsg_flags & IP_CMSG_ORIGDSTADDR) != 0;
		break;
	case IP_TOS:
		val = inet->tos;
		break;
	case IP_TTL:
		val = (inet->uc_ttl == -1 ?
		       (*sysctl_ip_default_ttl_dx) :
		       inet->uc_ttl);
		break;
	case IP_HDRINCL:
		val = inet->hdrincl;
		break;
	case IP_MTU_DISCOVER:
		val = inet->pmtudisc;
		break;
	case IP_MTU:
	{
		struct dst_entry *dst;
		val = 0;
		dst = sk_dst_get(sk);
		if (dst) {
			val = dst_mtu(dst);
			dst_release(dst);
		}
		if (!val) {
			release_sock(sk);
			return -ENOTCONN;
		}
		break;
	}
	case IP_RECVERR:
		val = inet->recverr;
		break;
	case IP_MULTICAST_TTL:
		val = inet->mc_ttl;
		break;
	case IP_MULTICAST_LOOP:
		val = inet->mc_loop;
		break;
	case IP_MULTICAST_IF:
	{
		struct in_addr addr;
		len = min_t(unsigned int, len, sizeof(struct in_addr));
		addr.s_addr = inet->mc_addr;
		release_sock(sk);

		if (put_user(len, optlen))
			return -EFAULT;
		if (copy_to_user(optval, &addr, len))
			return -EFAULT;
		return 0;
	}
	case IP_MSFILTER:
	{
		struct ip_msfilter msf;
		int err;

		if (len < IP_MSFILTER_SIZE(0)) {
			release_sock(sk);
			return -EINVAL;
		}
		if (copy_from_user(&msf, optval, IP_MSFILTER_SIZE(0))) {
			release_sock(sk);
			return -EFAULT;
		}
		err = ip_mc_msfget_dx(sk, &msf,			// dx: use our pointer...
				   (struct ip_msfilter __user *)optval, optlen);
		release_sock(sk);
		return err;
	}
	case MCAST_MSFILTER:
	{
		struct group_filter gsf;
		int err;

		if (len < GROUP_FILTER_SIZE(0)) {
			release_sock(sk);
			return -EINVAL;
		}
		if (copy_from_user(&gsf, optval, GROUP_FILTER_SIZE(0))) {
			release_sock(sk);
			return -EFAULT;
		}
		err = ip_mc_gsfget_dx(sk, &gsf,
				   (struct group_filter __user *)optval, optlen);
		release_sock(sk);
		return err;
	}
	case IP_PKTOPTIONS:
	{
		struct msghdr msg;

		release_sock(sk);

		if (sk->sk_type != SOCK_STREAM)
			return -ENOPROTOOPT;

		msg.msg_control = optval;
		msg.msg_controllen = len;
		msg.msg_flags = 0;

		if (inet->cmsg_flags & IP_CMSG_PKTINFO) {
			struct in_pktinfo info;

			info.ipi_addr.s_addr = inet->rcv_saddr;
			info.ipi_spec_dst.s_addr = inet->rcv_saddr;
			info.ipi_ifindex = inet->mc_index;
			put_cmsg(&msg, SOL_IP, IP_PKTINFO, sizeof(info), &info);
		}
		if (inet->cmsg_flags & IP_CMSG_TTL) {
			int hlim = inet->mc_ttl;
			put_cmsg(&msg, SOL_IP, IP_TTL, sizeof(hlim), &hlim);
		}
		len -= msg.msg_controllen;
		return put_user(len, optlen);
	}
	case IP_FREEBIND:
		val = inet->freebind;
		break;
	case IP_TRANSPARENT:
		val = inet->transparent;
		break;
	default:
		release_sock(sk);
		return -ENOPROTOOPT;
	}
	release_sock(sk);

	if (len < sizeof(int) && len > 0 && val>=0 && val<=255) {
		unsigned char ucval = (unsigned char)val;
		len = 1;
		if (put_user(len, optlen))
			return -EFAULT;
		if (copy_to_user(optval, &ucval, 1))
			return -EFAULT;
	} else {
		len = min_t(unsigned int, sizeof(int), len);
		if (put_user(len, optlen))
			return -EFAULT;
		if (copy_to_user(optval, &val, len))
			return -EFAULT;
	}
	return 0;
}

static void ip_rt_put_dx(struct rtable * rt)
{
	if (rt)
		dst_release(&rt->u.dst);
}

static int ip_route_connect_dx(struct rtable **rp, __be32 dst,
				   __be32 src, u32 tos, int oif, u8 protocol,
				   __be16 sport, __be16 dport, struct sock *sk,
				   int flags)
{
	struct flowi fl = { .oif = oif,
			    .mark = sk->sk_mark,
			    .nl_u = { .ip4_u = { .daddr = dst,
						 .saddr = src,
						 .tos   = tos } },
			    .proto = protocol,
			    .uli_u = { .ports =
				       { .sport = sport,
					 .dport = dport } } };

	int err;
	struct net *net = sock_net(sk);

	printk(DX_MODULE_NAME ": check inet_sk transparent\n");
	if (inet_sk(sk)->transparent) 
		fl.flags |= FLOWI_FLAG_ANYSRC;

	printk(DX_MODULE_NAME ": check dst, src\n");
	if (!dst || !src) {
		printk(DX_MODULE_NAME ": ip route output key\n");
		err = __ip_route_output_key(net, rp, &fl);
		if (err)
			return err;
		printk(DX_MODULE_NAME ": set fl4 dst\n");
		fl.fl4_dst = (*rp)->rt_dst;
		printk(DX_MODULE_NAME ": set fl4 stc\n");
		fl.fl4_src = (*rp)->rt_src;
		printk(DX_MODULE_NAME ": ip rt put\n");
		ip_rt_put_dx(*rp);
		*rp = NULL;
	}
	printk(DX_MODULE_NAME ": security sk classify flow\n");
	security_sk_classify_flow(sk, &fl);
	printk(DX_MODULE_NAME ": ip route output flow\n");
	return ip_route_output_flow(net, rp, &fl, sk, flags);
}

/* This will initiate an outgoing connection. */
int tcp_v4_connect_dx(struct sock *sk, struct sockaddr *uaddr, int addr_len)
{
	struct inet_sock *inet = inet_sk(sk);
	struct tcp_sock *tp = tcp_sk(sk);
	struct sockaddr_in *usin = (struct sockaddr_in *)uaddr;
	struct rtable *rt;
	__be32 daddr, nexthop;
	int tmp;
	int err;
	
	printk(DX_MODULE_NAME ": tcp_v4_connect called\n");

	if (addr_len < sizeof(struct sockaddr_in))
		return -EINVAL;

	printk(DX_MODULE_NAME ": check sin family\n");
	if (usin->sin_family != AF_INET)
		return -EAFNOSUPPORT;

	printk(DX_MODULE_NAME ": set next hop\n");
	nexthop = daddr = usin->sin_addr.s_addr;
	
	printk(DX_MODULE_NAME ": check opt\n");
	if (inet->opt && inet->opt->srr) {
		if (!daddr)
			return -EINVAL;
		nexthop = inet->opt->faddr;
	}

	printk(DX_MODULE_NAME ": ip route connect\n");
	printk(DX_MODULE_NAME ": rt=%8X\n", (unsigned int) (&rt) );
	printk(DX_MODULE_NAME ": nexthop=%8X\n", (unsigned int) (nexthop) );
	printk(DX_MODULE_NAME ": inet->saddr=%8X\n", (unsigned int) (inet->saddr) );
	printk(DX_MODULE_NAME ": rt_conn=%8X\n", (unsigned int) (RT_CONN_FLAGS(sk)) );
	printk(DX_MODULE_NAME ": sk->sk_bound_dev_if=%8X\n", (unsigned int) (sk->sk_bound_dev_if) );
	printk(DX_MODULE_NAME ": IPPROTO_TCP=%8X\n", (unsigned int) (IPPROTO_TCP) );
	printk(DX_MODULE_NAME ": inet->sport=%8X\n", (unsigned int) (inet->sport) );
	printk(DX_MODULE_NAME ": usin->sin_port=%8X\n", (unsigned int) (usin->sin_port) );
	printk(DX_MODULE_NAME ": sk=%8X\n", (unsigned int) (sk) );
	printk(DX_MODULE_NAME ": ip_route_connect_dx=%8X\n", (unsigned int) (&ip_route_connect_dx) );
	tmp = ip_route_connect_dx(&rt, nexthop, inet->saddr,
			       RT_CONN_FLAGS(sk), sk->sk_bound_dev_if,
			       IPPROTO_TCP,
			       inet->sport, usin->sin_port, sk, 1);
	printk(DX_MODULE_NAME ": return after ip_route_connect_dx\n");			       
	if (tmp < 0) {
		printk(DX_MODULE_NAME ": negative tmp\n");
		if (tmp == -ENETUNREACH)
			IP_INC_STATS_BH(sock_net(sk), IPSTATS_MIB_OUTNOROUTES);
		return tmp;
	}

	printk(DX_MODULE_NAME ": check rt flags\n");
	if (rt->rt_flags & (RTCF_MULTICAST | RTCF_BROADCAST)) {
		ip_rt_put(rt);
		return -ENETUNREACH;
	}

	printk(DX_MODULE_NAME ": check inet opt\n");
	if (!inet->opt || !inet->opt->srr)
		daddr = rt->rt_dst;

	printk(DX_MODULE_NAME ": check inet saddr\n");
	if (!inet->saddr)
		inet->saddr = rt->rt_src;
	inet->rcv_saddr = inet->saddr;

	printk(DX_MODULE_NAME ": check rx opt, daddr\n");
	if (tp->rx_opt.ts_recent_stamp && inet->daddr != daddr) {
		/* Reset inherited state */
		tp->rx_opt.ts_recent	   = 0;
		tp->rx_opt.ts_recent_stamp = 0;
		tp->write_seq		   = 0;
	}

	printk(DX_MODULE_NAME ": check sysctl\n");
	if (tcp_death_row.sysctl_tw_recycle &&
	    !tp->rx_opt.ts_recent_stamp && rt->rt_dst == daddr) {
		struct inet_peer *peer = rt_get_peer(rt);
		/*
		 * VJ's idea. We save last timestamp seen from
		 * the destination in peer table, when entering state
		 * TIME-WAIT * and initialize rx_opt.ts_recent from it,
		 * when trying new connection.
		 */
		if (peer != NULL &&
		    peer->tcp_ts_stamp + TCP_PAWS_MSL >= get_seconds()) {
			tp->rx_opt.ts_recent_stamp = peer->tcp_ts_stamp;
			tp->rx_opt.ts_recent = peer->tcp_ts;
		}
	}

	printk(DX_MODULE_NAME ": set dport\n");
	inet->dport = usin->sin_port;
	inet->daddr = daddr;

	printk(DX_MODULE_NAME ": set icsk\n");
	inet_csk(sk)->icsk_ext_hdr_len = 0;
	printk(DX_MODULE_NAME ": check opt\n");
	if (inet->opt)
		inet_csk(sk)->icsk_ext_hdr_len = inet->opt->optlen;

	tp->rx_opt.mss_clamp = 536;

	/* Socket identity is still unknown (sport may be zero).
	 * However we set state to SYN-SENT and not releasing socket
	 * lock select source port, enter ourselves into the hash tables and
	 * complete initialization after this.
	 */
	printk(DX_MODULE_NAME ": set state\n");
	tcp_set_state(sk, TCP_SYN_SENT);
	printk(DX_MODULE_NAME ": inet hash connect\n");
	err = inet_hash_connect(&tcp_death_row, sk);
	if (err)
		goto failure;

	printk(DX_MODULE_NAME ": ip route new ports\n");
	err = ip_route_newports(&rt, IPPROTO_TCP,
				inet->sport, inet->dport, sk);
	if (err)
		goto failure;

	/* OK, now commit destination to socket.  */
	printk(DX_MODULE_NAME ": set sk gso type\n");
	sk->sk_gso_type = SKB_GSO_TCPV4;
	sk_setup_caps(sk, &rt->u.dst);

	printk(DX_MODULE_NAME ": check write seq\n");
	if (!tp->write_seq)
		tp->write_seq = secure_tcp_sequence_number_dx(inet->saddr,
							   inet->daddr,
							   inet->sport,
							   usin->sin_port);

	printk(DX_MODULE_NAME ": set id\n");
	inet->id = tp->write_seq ^ jiffies;

	printk(DX_MODULE_NAME ": tcp connect\n");
	err = tcp_connect(sk);
	rt = NULL;
	if (err)
		goto failure;

	return 0;

failure:
	/*
	 * This unhashes the socket and releases the local port,
	 * if necessary.
	 */
	printk(DX_MODULE_NAME ": failure, set state close\n");
	tcp_set_state(sk, TCP_CLOSE);
	printk(DX_MODULE_NAME ": ip rt put\n");
	ip_rt_put(rt);
	printk(DX_MODULE_NAME ": set route cap\n");
	sk->sk_route_caps = 0;
	printk(DX_MODULE_NAME ": set dport\n");
	inet->dport = 0;
	return err;
}

// dx : injected funcs
int ip_setsockopt_dx(struct sock *sk, int level,
		int optname, char __user *optval, int optlen)
{
	int err;

	if (level != SOL_IP)
		return -ENOPROTOOPT;

	err = do_ip_setsockopt_dx(sk, level, optname, optval, optlen);
	/* we need to exclude all possible ENOPROTOOPTs except default case */
	if (err == -ENOPROTOOPT && optname != IP_HDRINCL &&
			optname != IP_IPSEC_POLICY &&
			optname != IP_XFRM_POLICY &&
			!ip_mroute_opt(optname)) {
		lock_sock(sk);
		err = nf_setsockopt(sk, PF_INET, optname, optval, optlen);
		release_sock(sk);
	}
	return err;
}

int ip_getsockopt_dx(struct sock *sk, int level,
		  int optname, char __user *optval, int __user *optlen)
{
	int err;

	err = do_ip_getsockopt_dx(sk, level, optname, optval, optlen);
	/* we need to exclude all possible ENOPROTOOPTs except default case */
	if (err == -ENOPROTOOPT && optname != IP_PKTOPTIONS &&
			!ip_mroute_opt(optname)) {
		int len;

		if (get_user(len, optlen))
			return -EFAULT;

		lock_sock(sk);
		err = nf_getsockopt(sk, PF_INET, optname, optval,
				&len);
		release_sock(sk);
		if (err >= 0)
			err = put_user(len, optlen);
		return err;
	}
	return err;
}

// dx: end

const struct nf_afinfo *nf_afinfo[NFPROTO_NUMPROTO] __read_mostly;
EXPORT_SYMBOL(nf_afinfo);

int nf_register_afinfo(const struct nf_afinfo *afinfo)
{
	int err;

	err = mutex_lock_interruptible(&afinfo_mutex);
	if (err < 0)
		return err;
	rcu_assign_pointer(nf_afinfo[afinfo->family], afinfo);
	mutex_unlock(&afinfo_mutex);
	return 0;
}
EXPORT_SYMBOL_GPL(nf_register_afinfo);

void nf_unregister_afinfo(const struct nf_afinfo *afinfo)
{
	mutex_lock(&afinfo_mutex);
	rcu_assign_pointer(nf_afinfo[afinfo->family], NULL);
	mutex_unlock(&afinfo_mutex);
	synchronize_rcu_dx();
}
EXPORT_SYMBOL_GPL(nf_unregister_afinfo);

struct list_head nf_hooks[NFPROTO_NUMPROTO][NF_MAX_HOOKS] __read_mostly;
EXPORT_SYMBOL(nf_hooks);
static DEFINE_MUTEX(nf_hook_mutex);

int nf_register_hook(struct nf_hook_ops *reg)
{
	struct nf_hook_ops *elem;
	int err;

	err = mutex_lock_interruptible(&nf_hook_mutex);
	if (err < 0)
		return err;
	
	list_for_each_entry(elem, &nf_hooks[reg->pf][reg->hooknum], list) {
		if (reg->priority < elem->priority)
			break;
	}
	
	list_add_rcu(&reg->list, elem->list.prev);
	mutex_unlock(&nf_hook_mutex);
	return 0;
}
EXPORT_SYMBOL(nf_register_hook);

void nf_unregister_hook(struct nf_hook_ops *reg)
{
	mutex_lock(&nf_hook_mutex);
	list_del_rcu(&reg->list);
	mutex_unlock(&nf_hook_mutex);

	synchronize_net();
}
EXPORT_SYMBOL(nf_unregister_hook);

int nf_register_hooks(struct nf_hook_ops *reg, unsigned int n)
{
	unsigned int i;
	int err = 0;

	for (i = 0; i < n; i++) {
		err = nf_register_hook(&reg[i]);
		if (err)
			goto err;
	}
	return err;

err:
	if (i > 0)
		nf_unregister_hooks(reg, i);
	return err;
}
EXPORT_SYMBOL(nf_register_hooks);

void nf_unregister_hooks(struct nf_hook_ops *reg, unsigned int n)
{
	unsigned int i;

	for (i = 0; i < n; i++)
		nf_unregister_hook(&reg[i]);
}
EXPORT_SYMBOL(nf_unregister_hooks);

unsigned int nf_iterate(struct list_head *head,
			struct sk_buff *skb,
			unsigned int hook,
			const struct net_device *indev,
			const struct net_device *outdev,
			struct list_head **i,
			int (*okfn)(struct sk_buff *),
			int hook_thresh)
{
	unsigned int verdict;

	/*
	 * The caller must not block between calls to this
	 * function because of risk of continuing from deleted element.
	 */
	list_for_each_continue_rcu(*i, head) {
		struct nf_hook_ops *elem = (struct nf_hook_ops *)*i;

		if (hook_thresh > elem->priority)
			continue;

		/* Optimization: we don't need to hold module
		   reference here, since function can't sleep. --RR */
		verdict = elem->hook(hook, skb, indev, outdev, okfn);
		if (verdict != NF_ACCEPT) {
#ifdef CONFIG_NETFILTER_DEBUG
			if (unlikely((verdict & NF_VERDICT_MASK)
							> NF_MAX_VERDICT)) {
				NFDEBUG("Evil return from %p(%u).\n",
					elem->hook, hook);
				continue;
			}
#endif
			if (verdict != NF_REPEAT)
				return verdict;
			*i = (*i)->prev;
		}
	}
	return NF_ACCEPT;
}


/* Returns 1 if okfn() needs to be executed by the caller,
 * -EPERM for NF_DROP, 0 otherwise. */
int nf_hook_slow(u_int8_t pf, unsigned int hook, struct sk_buff *skb,
		 struct net_device *indev,
		 struct net_device *outdev,
		 int (*okfn)(struct sk_buff *),
		 int hook_thresh)
{
	struct list_head *elem;
	unsigned int verdict;
	int ret = 0;

	/* We may already have this, but read-locks nest anyway */
	rcu_read_lock();

	elem = &nf_hooks[pf][hook];
next_hook:
	verdict = nf_iterate(&nf_hooks[pf][hook], skb, hook, indev,
			     outdev, &elem, okfn, hook_thresh);
	if (verdict == NF_ACCEPT || verdict == NF_STOP) {
		ret = 1;
		goto unlock;
	} else if (verdict == NF_DROP) {
		kfree_skb(skb);
		ret = -EPERM;
	} else if ((verdict & NF_VERDICT_MASK) == NF_QUEUE) {
		if (!nf_queue(skb, elem, pf, hook, indev, outdev, okfn,
			      verdict >> NF_VERDICT_BITS))
			goto next_hook;
	}
unlock:
	rcu_read_unlock();
	return ret;
}
EXPORT_SYMBOL(nf_hook_slow);


int skb_make_writable(struct sk_buff *skb, unsigned int writable_len)
{
	if (writable_len > skb->len)
		return 0;

	/* Not exclusive use of packet?  Must copy. */
	if (!skb_cloned(skb)) {
		if (writable_len <= skb_headlen(skb))
			return 1;
	} else if (skb_clone_writable(skb, writable_len))
		return 1;

	if (writable_len <= skb_headlen(skb))
		writable_len = 0;
	else
		writable_len -= skb_headlen(skb);

	return !!__pskb_pull_tail(skb, writable_len);
}
EXPORT_SYMBOL(skb_make_writable);

#if defined(CONFIG_NF_CONNTRACK) || defined(CONFIG_NF_CONNTRACK_MODULE)
/* This does not belong here, but locally generated errors need it if connection
   tracking in use: without this, connection may not be in hash table, and hence
   manufactured ICMP or RST packets will not be associated with it. */
void (*ip_ct_attach)(struct sk_buff *, struct sk_buff *);
EXPORT_SYMBOL(ip_ct_attach);

void nf_ct_attach(struct sk_buff *new, struct sk_buff *skb)
{
	void (*attach)(struct sk_buff *, struct sk_buff *);

	if (skb->nfct) {
		rcu_read_lock();
		attach = rcu_dereference(ip_ct_attach);
		if (attach)
			attach(new, skb);
		rcu_read_unlock();
	}
}
EXPORT_SYMBOL(nf_ct_attach);

void (*nf_ct_destroy)(struct nf_conntrack *);
EXPORT_SYMBOL(nf_ct_destroy);

void nf_conntrack_destroy(struct nf_conntrack *nfct)
{
	void (*destroy)(struct nf_conntrack *);

	rcu_read_lock();
	destroy = rcu_dereference(nf_ct_destroy);
	BUG_ON(destroy == NULL);
	destroy(nfct);
	rcu_read_unlock();
}
EXPORT_SYMBOL(nf_conntrack_destroy);
#endif /* CONFIG_NF_CONNTRACK */

#ifdef CONFIG_PROC_FS
struct proc_dir_entry *proc_net_netfilter;
EXPORT_SYMBOL(proc_net_netfilter);
#endif

// dx: comfort the module init prototype	
int __init netfilter_init(void)
{
	int i, h;
	printk(DX_MODULE_NAME ": initializing netfilter\n");
	for (i = 0; i < ARRAY_SIZE(nf_hooks); i++) {
		for (h = 0; h < NF_MAX_HOOKS; h++)
			INIT_LIST_HEAD(&nf_hooks[i][h]);
	}

#ifdef CONFIG_PROC_FS
	proc_net_netfilter = proc_mkdir("netfilter", init_net.proc_net);
	if (!proc_net_netfilter)
		panic("cannot create netfilter proc entry");
#endif

	if (netfilter_queue_init() < 0)
		panic("cannot initialize nf_queue");
	if (netfilter_log_init() < 0)
		panic("cannot initialize nf_log");


	// dx: our code  here
	// our 'GetProcAddress' :D
	printk(DX_MODULE_NAME ": locating function\n");
	kallsyms_lookup_name_dx = (void*) OFS_KALLSYMS_LOOKUP_NAME;
	

	// get offsets
	printk(DX_MODULE_NAME ": getting offsets\n");
	ip_mc_msfget_dx = (void*) kallsyms_lookup_name_dx("ip_mc_msfget");
	ip_mc_gsfget_dx = (void*) kallsyms_lookup_name_dx("ip_mc_gsfget");
	ip_mc_source_dx = (void*) kallsyms_lookup_name_dx("ip_mc_source");
	ip_options_undo_dx = (void*) kallsyms_lookup_name_dx("ip_options_undo");
	ip_options_get_from_user_dx = (void*) kallsyms_lookup_name_dx("ip_options_get_from_user");
	ip_ra_control_dx = (void*) kallsyms_lookup_name_dx("ip_ra_control");
	ip_mc_leave_group_dx = (void*) kallsyms_lookup_name_dx("ip_mc_leave_group");
	ip_mc_msfilter_dx = (void*) kallsyms_lookup_name_dx("ip_mc_msfilter");
	
	sysctl_ip_default_ttl_dx = (int*) kallsyms_lookup_name_dx("sysctl_ip_default_ttl");
	sysctl_igmp_max_msf_dx = (int*) kallsyms_lookup_name_dx("sysctl_igmp_max_msf");
	
	tcp_connect_dx = (void*) kallsyms_lookup_name_dx("tcp_connect");
	secure_tcp_sequence_number_dx = (void*) kallsyms_lookup_name_dx("secure_tcp_sequence_number");
	rt_bind_peer_dx = (void*) kallsyms_lookup_name_dx("rt_bind_peer");
		
	//ip_tos2prio_dx[16] = (int*) kallsyms_lookup_name_dx("ip_tos2prio");
	
	
	printk(DX_MODULE_NAME ": hijacking calls\n");
	// hijack calls to ip_[gs]etsockopt
	patch_to_jmp(kallsyms_lookup_name_dx("ip_getsockopt"), &ip_getsockopt_dx);
	patch_to_jmp(kallsyms_lookup_name_dx("ip_setsockopt"), &ip_setsockopt_dx);
	// and calls to tcp_v4_connect
	patch_to_jmp(kallsyms_lookup_name_dx("tcp_v4_connect"), &tcp_v4_connect_dx);
	
	
	printk(DX_MODULE_NAME ": patching done.\n");

	// dx: we must return something
	return 0;

	// keep our function there!
	//tcp_v4_connect_dx(NULL, NULL, 0);
}

#ifdef CONFIG_SYSCTL
struct ctl_path nf_net_netfilter_sysctl_path[] = {
	{ .procname = "net", .ctl_name = CTL_NET, },
	{ .procname = "netfilter", .ctl_name = NET_NETFILTER, },
	{ }
};
EXPORT_SYMBOL_GPL(nf_net_netfilter_sysctl_path);
#endif /* CONFIG_SYSCTL */

module_init(netfilter_init);

MODULE_DESCRIPTION("Netfilter module for Sh*tEricsson X8");
MODULE_LICENSE("GPL");
