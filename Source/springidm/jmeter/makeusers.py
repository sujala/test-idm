#! /usr/bin/env python

f = open('newusers.csv', 'w')

for i in range(1000000):
   f.write('LoadTestUser8-{}\n'.format(i))

f.close()
