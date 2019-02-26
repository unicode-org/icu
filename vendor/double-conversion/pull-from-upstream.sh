#!/bin/bash
# © 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

if [[ -z $1 ]]; then
	echo "Pass the current version tag of double-conversion as the first argument to this script";
	echo "To pull the latest changes, use 'master'"
	exit 1;
fi

filename="$1.tar.gz"
url="https://github.com/google/double-conversion/archive/$1.tar.gz";
upstream_root="$(dirname "$0")/upstream";
icu4c_i18n_root="$(dirname "$0")/../../icu4c/source/i18n";

echo "Will download $url";
echo "Will expand into $upstream_root";
read -p "Press Enter to continue or s to skip: " ch;

if [ "$ch" != "s" ]; then
	rm -f $filename;
	wget $url;
	rm -r "$upstream_root"; # in case any files were deleted in the new version
	mkdir "$upstream_root";
	tar zxf $filename --strip 1 -C "$upstream_root";
	rm $filename;
	echo "upstream updated and $filename removed";
fi

echo "Will apply diffs to $icu4c_i18n_root";
read -p "Press Enter to continue or s to skip: " ch;

do_patch() {
	vendor_path="$upstream_root/double-conversion/$1";
	icu4c_path="$icu4c_i18n_root/$2";
	git diff --patch "$vendor_path" | patch --merge "$icu4c_path";
}

do_patch_prefix_extension() {
	do_patch "$1.$2" "double-conversion-$1.$3";
}

do_patch_extension() {
	do_patch "$1.$2" "$1.$3";
}

if [ "$ch" != "s" ]; then
	do_patch_prefix_extension bignum-dtoa cc cpp;
	do_patch_prefix_extension bignum-dtoa h h;
	do_patch_prefix_extension bignum cc cpp;
	do_patch_prefix_extension bignum h h;
	do_patch_prefix_extension cached-powers cc cpp;
	do_patch_prefix_extension cached-powers h h;
	do_patch_prefix_extension diy-fp cc cpp;
	do_patch_prefix_extension diy-fp h h;
	do_patch_prefix_extension fast-dtoa cc cpp;
	do_patch_prefix_extension fast-dtoa h h;
	do_patch_prefix_extension ieee h h;
	do_patch_prefix_extension utils h h;
	do_patch_extension double-conversion cc cpp;
	do_patch_extension double-conversion h h;
fi
