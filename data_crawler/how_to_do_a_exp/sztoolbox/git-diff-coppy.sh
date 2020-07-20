#!/bin/bash
# Target directory
TARGET=$3
echo "Finding and copying files and folders to $TARGET"
for i in $(git diff --name-only $1 $2)
    do
        # First create the target directory, if it doesn't exist.
        git checkout $1
        mkdir -p "$TARGET/from/$(dirname $i)"
         # Then copy over the file.
        cp "$i" "$TARGET/from/$i"
        git checkout $2
        mkdir -p "$TARGET/to/$(dirname $i)"
         # Then copy over the file.
        cp "$i" "$TARGET/to/$i"
        
        echo "get $i"
    done
echo "Files copied to target directory";
