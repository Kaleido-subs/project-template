#!/bin/sh

git config --local filter.ass-clean.clean "scripts/clean_project_garbage.sh"
echo "Successfully configured ass-clean filter!"
