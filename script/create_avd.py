#!/usr/bin/env python

import sys, os, argparse, subprocess, platform, time

def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('number', help='number of AVDs you want to create',
      type=int)
  parser.add_argument('path', help='path to your Android SDK')
  parser.add_argument('-a', '--arch', help='the architecture (x86 or arm)',
      default='x86')
  parser.add_argument('-d', '--device', help='Device name or ID',
      default='2.7in QVGA slider')
  parser.add_argument('-v', '--api', help='Android API version',
      type=int, default=19)

  args = parser.parse_args()
  arch = abi = args.arch
  api = args.api
  device = args.device
  path = args.path

  if arch == "arm":
    abi = "armeabi-v7a"
  elif arch != "x86":
    print "Supported architectures are x86 (default) or arm"
    return

  android_version = 'android-' + str(api)
  image_path = 'system-images;' + android_version + ';google_apis;' + abi

  print 'sdkmanager "' + image_path + '"'
  subprocess.call(["sdkmanager", image_path])

  for i in range(args.number):
    n = str(i)
    cmd = [ 'avdmanager', 'create', 'avd', '-n', 'avd' + n, '-b', abi, '-f',
            '-c', '64M', '-k', image_path, '-d', device ]
    print
    print ' '.join(cmd)
    if platform.system() == "Windows":
      p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    else:
      p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    p.communicate(input='no\n')
    print
    print "Waiting for avdmanager to exit"
    p.wait()

if __name__ == "__main__":
  main()
