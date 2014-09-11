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

### AMI Generation

If you want to generate an [Amazon Web Service's](http://aws.amazon.com/) (AWS) [Amazon Machine Image](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html) (AMI) you can use
Packer to handle the provisioning.

Use the following instructions to do so:

1.  Create an AWS account if you do not have one already
2.  Get the [API keys](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html) for a new user or an existing user in your AWS account
2.  [Download and install Packer](http://www.packer.io/)
3.  Navigate to the `deployment/packer` directory
4.  Copy the `open-transit-vars.json.example` file and save it as `open-transit-vars.json`
5.  Edit `open-transit-vars.json` with the API keys downloaded in step 2
6.  Run the following command to generate a new AMI: `packer build -var-file=open-transit-vars.json open-transit-indicators.json` PLEASE NOTE: Running this command will cause resources to be created in AWS and will cost money
7.  Once the process finishes installing (could take up to an hour), make note of the AMI ID
8.  Launch the AMI using the AWS EC2 management console and browse to the public DNS hostname provided by Amazon