#!/bin/sh

# sadly hacked together install script; really only used to bootstrap rpm or debs
# apologies in advance to anyone who has to debug this

prefix_default=/usr/local
root_default=""
binuser=flowvisor
bingroup=flowvisor
 
install="$sudo install"
base=`dirname $0`/..
scriptd=$base/scripts
libs=$base/lib
dist=$base/dist
#jni=$base/jni
config=flowvisor-config.json
#verbose=-v

usage="$0 [-p prefix_dir] [-u flowvisor_user] [-g flowvisor_group] [-r root_dir]"
usage_str="p:u:g:r:"

while getopts $usage_str opt; do
    case $opt in
    p)
        prefix=$OPTARG
        echo "Set prefix to '$prefix'" >&2
    ;;
    u)
        fvuser=$OPTARG
        echo "Set fvuser to '$fvuser'" >&2
    ;;
    g)
        fvgroup=$OPTARG
        echo "Set fvgroup to '$fvgroup'" >&2
    ;;
    r)
        root=$OPTARG
        echo "Set root to '$root'" >&2
    ;;
    \?)
        echo "Invalid option: -$OPTARG" >&2
        cat << EOF  >&2
        Usage:
        $usage
            defaults:
                prefix_dir=$prefix_default
                fvuser=$fvuser_default
                fvgroup=$fvgroup_default
                root=$root_default
EOF
        exit 1
    ;;
esac
done

echo "Using source dir: $base"

echo Installing FlowVisor into $root$prefix with prefix=$prefix as user/group ${fvuser}:${fvgroup}

bin_SCRIPTS="\
    fvctl \
    "

sbin_SCRIPTS="\
    fvconfig \
    flowvisor \
    "

LIBS="\
    commons-collections-3.2.1.jar \
    commons-dbcp-1.4.jar \
    commons-logging-1.1.jar \
    commons-pool-1.5.6.jar \
    openflow.jar \
    jsse.jar \
    ws-commons-util-1.0.2.jar \
    xmlrpc-client-3.1.3.jar \
    xmlrpc-common-3.1.3.jar \
    xmlrpc-server-3.1.3.jar \
    asm-3.0.jar \
    cglib-2.2.jar \
    commons-codec-1.4.jar \
    gson-2.0.jar \
    jetty-continuation-7.0.2.v20100331.jar \
    jetty-http-7.0.2.v20100331.jar \
    jetty-io-7.0.2.v20100331.jar \
    jetty-security-7.0.2.v20100331.jar \
    jetty-server-7.0.2.v20100331.jar \
    jetty-util-7.0.2.v20100331.jar \
    derby.jar \
    derbytools.jar
    servlet-api-2.5.jar \
    jna.jar \
    log4j-1.2.17.jar \
    syslog4j-0.9.46-bin.jar\
    slf4j-api-1.7.2.jar\
    slf4j-log4j12-1.7.2.jar
    "

DOCS="\
    README
    README.dev
    INSTALL
    "

owd=`pwd`
cd $scriptd

for script in $bin_SCRIPTS $sbin_SCRIPTS envs ; do 
    echo Updating $script.sh to $script
    sed -e "s!#base=PREFIX!base=$prefix!" -e "s!#configbase=PREFIX!configbase=$prefix!"< $script.sh > $script
done

echo Creating directories

for d in bin sbin libexec/flowvisor etc share/man/man1 share/man/man8 share/doc/flowvisor share/db/flowvisor ; do 
    echo Creating $prefix/$d
    $install $verbose --owner=$binuser --group=$bingroup --mode=755 -d $root$prefix/$d
done

for d in /etc/init.d ; do
    if [ ! -d $root$d ] ; then
        echo Creating $d
        $install $verbose --owner=$binuser --group=$bingroup --mode=755 -d $root$d
    fi
done

for d in /var/log/flowvisor ; do
    if [ ! -d $root$d ] ; then
        echo Creating $d
        $install $verbose --owner=$fvuser --group=$fvgroup --mode=755 -d $root$d
    fi
done



echo "Creating /etc/flowvisor (owned by user=$fvuser  group=$fvgroup)"
$install $verbose --owner=$fvuser --group=$fvgroup --mode=2755 -d $root/etc/flowvisor

echo Installing scripts
$install $verbose --owner=$binuser --group=$bingroup --mode=755 $bin_SCRIPTS $root$prefix/bin
$install $verbose --owner=$binuser --group=$bingroup --mode=755 $sbin_SCRIPTS $root$prefix/sbin

echo "Installing SYSV startup script (not enabled by default)"
cp fv-startup.sh fv-startup
sed -i -e "s/FVUSER/$fvuser/" fv-startup
sed -i -e "s,PREFIX,$prefix," fv-startup
$install $verbose --owner=$binuser --group=$bingroup --mode=755 fv-startup  $root/etc/init.d/flowvisor


echo Installing jars
cd $owd
cd $libs
$install $verbose --owner=$binuser --group=$bingroup --mode=644 $LIBS $root$prefix/libexec/flowvisor

echo Installing flowvisor.jar
cd $owd
cd $dist
$install $verbose --owner=$binuser --group=$bingroup --mode=644 flowvisor.jar  $root$prefix/libexec/flowvisor

echo Installing manpages
cd $owd
cd doc
$install $verbose --owner=$binuser --group=$bingroup --mode=644 fvctl.1  $root$prefix/share/man/man1
$install $verbose --owner=$binuser --group=$bingroup --mode=644 fvconfig.1  $root$prefix/share/man/man1
$install $verbose --owner=$binuser --group=$bingroup --mode=644 flowvisor.8  $root$prefix/share/man/man8
# do we need to run makewhatis manually here? I think it's a cronjob on most systems


echo Installing FlowVisorDB
cd $owd
envs=$base/scripts/envs.sh
flowvisor_db=`dirname $0`/../scripts/FlowVisorDB.sql
if [ -z $flowvisor_db ]; then
    echo "Could not find database script file; your release is probably corrupt..." >&2
    exit 
fi
if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 $scriptd/derby.properties $root/etc/flowvisor/derby.properties
JAVA=`which java`
CHOWN=`which chown`
$JAVA -Dderby.system.home=$root$prefix/share/db/flowvisor -cp $classpath org.apache.derby.tools.ij $flowvisor_db > /dev/null
$CHOWN -R $fvuser:$fvgroup $root$prefix/share/db/flowvisor

echo Installing configs
cd $owd
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 $scriptd/envs $root/etc/flowvisor/envs.sh
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 $scriptd/fvlog.config $root/etc/flowvisor/fvlog.config

echo Installing documentation
cd $owd
$install $verbose --owner=$binuser --group=$bingroup --mode=644 $DOCS $root$prefix/share/doc/flowvisor
$CHOWN -R $fvuser:$fvgroup $root$prefix/share/db/flowvisor
#if [ ! -f $root/etc/flowvisor/config.json ] ; then 
#    echo Generating a default config FlowVisor config
#    install_root=$root $root$prefix/sbin/fvconfig generate $root/etc/flowvisor/config.json localhost flowvisor 6633 8080 
#    $CHOWN -R $fvuser:$fvgroup $root/etc/flowvisor/config.json
#else
#    echo "Config file already exists, not touching anything... including db"
#fi
