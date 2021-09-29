#!/bin/bash
# © 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

if [[ -z $1 ]]; then
	echo "Pass the current version tag of double-conversion as the first argument to this script";
	echo "To pull the latest changes, use 'master'"
	exit 1;
fi

url="https://github.com/google/double-conversion/archive/$1.tar.gz";
upstream_root="$(dirname "$0")/upstream";
icu4c_i18n_root="$(dirname "$0")/../../icu4c/source/i18n";
tmpdir=`mktemp -d`;
filename="$tmpdir/$1.tar.gz"
upstream_root_tmp="$tmpdir/upstream"
patch_root_tmp="$tmpdir/patches"

echo "Will download $url";
read -p "Press Enter to continue or s to skip: " ch;

if [ "$ch" != "s" ]; then
	wget -O "$filename" "$url";
	mkdir "$upstream_root_tmp";
	tar zxf $filename --strip 1 -C "$upstream_root_tmp";
fi

echo "Will apply diffs to $icu4c_i18n_root";
read -p "Press Enter to continue or s to skip: " ch;

do_patch() {
	old_vendor_path="$upstream_root/double-conversion/$1";
	new_vendor_path="$upstream_root_tmp/double-conversion/$1";
	icu4c_path="$icu4c_i18n_root/$2";
	tmp_path="$patch_root_tmp/$2.patch";
	diff -u "$old_vendor_path" "$icu4c_path" > "$tmp_path";
	cp "$new_vendor_path" "$icu4c_path";
	patch --merge "$icu4c_path" < "$tmp_path";
}

do_patch_prefix_extension() {
	do_patch "$1.$2" "double-conversion-$1.$3";
}

do_patch_extension() {
	do_patch "$1.$2" "$1.$3";
}

if [ "$ch" != "s" ]; then
	mkdir "$patch_root_tmp";
	do_patch_prefix_extension bignum cc cpp;
	do_patch_prefix_extension bignum h h;
	do_patch_prefix_extension bignum-dtoa cc cpp;
	do_patch_prefix_extension bignum-dtoa h h;
	do_patch_prefix_extension cached-powers cc cpp;
	do_patch_prefix_extension cached-powers h h;
	do_patch_prefix_extension diy-fp h h;
	do_patch_prefix_extension double-to-string cc cpp;
	do_patch_prefix_extension double-to-string h h;
	do_patch_prefix_extension fast-dtoa cc cpp;
	do_patch_prefix_extension fast-dtoa h h;
	do_patch_prefix_extension ieee h h;
	do_patch_prefix_extension string-to-double cc cpp;
	do_patch_prefix_extension string-to-double h h;
	do_patch_prefix_extension strtod cc cpp;
	do_patch_prefix_extension strtod h h;
	do_patch_prefix_extension utils h h;
	do_patch_extension double-conversion h h;
fi

echo "Will save pristine copy into $upstream_root";
read -p "Press Enter to continue or s to skip: " ch;

if [ "$ch" != "s" ]; then
	rm -r "$upstream_root"; # in case any files were deleted in the new version
	cp -r "$upstream_root_tmp" "$upstream_root";
fi

echo "Temporary files have been saved in $tmpdir";
read -p "Press Enter to delete the directory or s to skip: " ch;

if [ "$ch" != "s" ]; then
	rm -rf "$tmpdir";
fi
