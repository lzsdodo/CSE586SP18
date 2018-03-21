#!/usr/bin/python2

import os, re, subprocess

def check_config(fn):
	with open(fn, 'r') as f:
		for line in f:
			key = line.split('=')
			if key[0] == 'hw.keyboard' and key[1].startswith('yes'):
				return True
	return False

def main():
	p = subprocess.Popen('avdmanager list avd', stdout=subprocess.PIPE, shell=True, universal_newlines=True)
	(data, errors) = p.communicate()
	name = None
	path = None
	for line in data.split("\n"):
		if line == '---------':
			name = None
			path = None
			continue

		match = re.match(' +Name: (.*)$', line)
		if match != None:
			name = match.group(1)

		match = re.match(' +Path: (.*)$', line)
		if match != None:
			path = match.group(1)

		if name and path:
			# We have found an AVD named 'name' at path 'path'
			if not re.match('avd[0-4]$', name):
				# This isn't our AVD, move along
				name = None
				path = None
				continue

			# This is our AVD, check if we need to update it
			config = os.path.join(path, 'config.ini')
			if check_config(config):
				# This config is already updated
				print 'AVD %s appears to have been updated already, skipping' % name
				name = None
				path = None
				continue

			# Update this configuration
			with open(config, 'a') as f:
				print 'Updating AVD %s' % name
				print >> f, 'hw.keyboard=yes'

			name = None
			path = None

if __name__ == "__main__":
	main()