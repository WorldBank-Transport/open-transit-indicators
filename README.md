open-transit-indicators
=======================

An open-source tool to support transport agencies in planning and managing public transit systems

Build Status
----------------------
[![Travis CI Build Status](https://travis-ci.org/WorldBank-Transport/open-transit-indicators.svg?branch=develop)](https://travis-ci.org/WorldBank-Transport/open-transit-indicators)

[![Selenium Test Status](https://saucelabs.com/buildstatus/azavea-oti)](https://saucelabs.com/u/azavea-oti)

Installation
----------------------

### Vagrant

If you're using Vagrant, then installation is as simple as cloning the repo and then
issuing `vagrant up` from the root of the repository directory.

To change the default amount of memory allocated to the vagrant machine, set the environment
variable `OTI_VAGRANT_MEMORY` to the preferred size, in MB.

### Server installation

If you're installing directly onto a dedicated server, follow these steps after
cloning the repo:

1. Edit `deployment/provision.sh` and change the line saying:
`PROJECT_ROOT="/projects/open-transit-indicators"`.
The text `/projects/open-transit-indicators` should be replaced with the absolute path of
the location where you've cloned the repository. For example, if your repository folder is
located in `/home/myusername/open-transit-indicators`, the line should become
`PROJECT_ROOT="/home/myusername/open-transit-indicators"`

2. From the repository folder (project directory), issue the command:
`sudo ./deployment/provision.sh development`. You may need to enter your password.
This will create a development installation of the program. If you are simply planning to
use the program (rather than make changes to it), run `sudo ./deployment/provision.sh production`
instead.
