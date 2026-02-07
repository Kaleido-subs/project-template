#!/bin/sh

sed -e "/^; /d" -e '/^\[Aegisub Project Garbage/,/^$/d'
