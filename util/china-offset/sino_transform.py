#!/usr/bin/env python

# Source: https://on4wp7.codeplex.com/SourceControl/changeset/view/21455#EvilTransform.cs
# Python source: http://blog.snapdragon.cc/2013/07/17/china-gps-offset-problem/
# Copyright (C) 1000 - 9999 Somebody Anonymous
# NO WARRANTY OR GUARANTEE

import math

a = 6378245.0
ee = 0.00669342162296594323


def _transformLat(x, y):
    ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * math.sqrt(math.fabs(x))
    ret += (20.0 * math.sin(6.0 * x * math.pi) + 20.0 * math.sin(2.0 * x * math.pi)) * 2.0 / 3.0
    ret += (20.0 * math.sin(y * math.pi) + 40.0 * math.sin(y / 3.0 * math.pi)) * 2.0 / 3.0
    ret += (160.0 * math.sin(y / 12.0 * math.pi) + 320 * math.sin(y * math.pi / 30.0)) * 2.0 / 3.0
    return ret


def _transformLon(x, y):
    ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * math.sqrt(math.fabs(x))
    ret += (20.0 * math.sin(6.0 * x * math.pi) + 20.0 * math.sin(2.0 * x * math.pi)) * 2.0 / 3.0
    ret += (20.0 * math.sin(x * math.pi) + 40.0 * math.sin(x / 3.0 * math.pi)) * 2.0 / 3.0
    ret += (150.0 * math.sin(x / 12.0 * math.pi) + 300.0 * math.sin(x / 30.0 * math.pi)) * 2.0 / 3.0
    return ret


def transform(wgLat, wgLon):
    dLat = _transformLat(wgLon - 105.0, wgLat - 35.0)
    dLon = _transformLon(wgLon - 105.0, wgLat - 35.0)
    radLat = wgLat / 180.0 * math.pi
    magic = math.sin(radLat)
    magic = 1 - ee * magic * magic
    sqrtMagic = math.sqrt(magic)
    dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * math.pi)
    dLon = (dLon * 180.0) / (a / sqrtMagic * math.cos(radLat) * math.pi)
    mgLat = wgLat + dLat
    mgLon = wgLon + dLon
    return mgLat, mgLon
