# Enhanced Snapshots

![Enhanced Snapshots Logo](https://cloud.githubusercontent.com/assets/1557544/10324458/c19466ca-6c57-11e5-8318-a2eb1cd9e99b.png)

[![Version][github-image]][github-url]
[![Build Status][travis-image]][travis-url]

*Table of contents*
* [Product Description](#product-description)
* [Key Features](#key-features)
* [Limitations](#limitations)
* [Quick start](#quick-start)
* [Getting Started](#getting-started)
* [Management Tasks](#management-tasks)
* [Removing the Enhanced Snapshots system](#removing-the-enhanced-snapshots-system)
* [IAM Role creation (optional)](#iam-role-creation-optional)
* [Logging](#loggings)
* [License](#license)

# Product Description
Enhanced Snapshots from Sungard Availability Services | Labs manages Amazon EBS snapshots and performs data deduplication to Amazon Simple Storage (Amazon S3). Users of Amazon We Services (AWS) can leverage the product to:

* Reduce the cost of storing snapshots
* Reduce the time that IT engineers spend on routine snapshot management tasks
* Schedule recurring snapshots

Deduplication is run across all enabled snapshots in the AWS region, thus decreasing the amount of the total stored data. Therefore, the resulting deduplicated blocks are stored in Amazon S3 at a much lower cost than the standard AWS EBS snapshots and you pay less for the long-term retention of the snapshot-based data in AWS.

Using an intuitive interface, you can easily automate such routine tasks like creating snapshots or deleting old backups. Since these tasks are automated, risks associated with the human error are minimized.

Technical support is available only starting with Enhanced Snapshots 2.0.0. If you have any comments or suggestions, you can create a Github issue. Customer support may be added in a future release.

Enhanced Snapshots is open sourced and licensed under the Apache License 2.0. Use of the Enhanced Snapshots software is free. You pay only for the underlying infrastructure that is required to support it.

The Enhanced Snapshots tool is available after launching the Amazon Machine Image (AMI) from the [enhanced snapshots market place](https://aws.amazon.com/marketplace/pp/B01CIWY4UO) by selecting the **Enhanced Snapshots tool creation stack** option or by creating the **es-admin** role while using the single AMI option. You can use the cloud formation template mentioned in the [Quick start](https://github.com/sungardas/enhanced-snapshots#quick-start) section to create the **es-admin** role. Similarly, you can uninstall the created EC2 instance and associated resources. However, after performing the uninstallation, you will not be removed from the AWS subscription for the SungardAS provided marketplace products. For more details, refer to [Removing the enhanced snapshots system](https://github.com/sungardas/enhanced-snapshots#removing-the-enhanced-snapshots-system).

# Key Features
## Backup & Recovery
* Create backups  of the EBS volumes.
* Perform instance recovery from the historical backups.
* Store backups of the deleted volumes.
* Quickly initiate the backups recovery.
* Run backup and recovery tasks in parallel.

## Schedule Policy
* Scheduled tasks by minute, hour, day, week, and month.
* Full support of the CRON expressions.

## Retention Policy
* Automatically delete older backups based on the original volume size, count, or age.

## Management
* Simple and intuitive wizard for initial setup process.
* Admin and User roles.
* Migration to another EC2 instance with full data restoration.
* Single sign-on. For more information on how to configure single sign-on, see the [Configuration Management](https://github.com/SungardAS/enhanced-snapshots/blob/master/ConfurationManagement.md) file.
* Configure and view runtime logs.
* View task history.


# Limitations
* No support for managing the volumes that use the OS level RAID.
* To avoid a significant storage overhead, the EBS volumes, which use the EBS encryption, must be [pre-warmed](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-prewarm.html).

# Quick start
 If you are using **Enhanced Snapshots tool creation stack** in the marketplace, the **es-admin** role is automatically created. For a single AMI option from the marketplace, create the **es-admin** role using the cloud formation template as prerequisite [es-admin-role](https://github.com/SungardAS/condensation-particles/blob/master/particles/cftemplates/sungardas_enhanced_snapshots_admin_role.template.json)
 To launch a CloudFormation stack based on the template, choose a region in which you will perform the deployments. In this region, you will need the following information:


 * [EC2 keypair](https://us-east-1.console.aws.amazon.com/ec2/v2/home?#KeyPairs).
 * [VPC ID](https://console.aws.amazon.com/vpc/home?#vpcs:) (not required if you are using the Simple Stack option below).
 * IP prefix from which the inbound HTTP and HTTPS connections are allowed. If you are not sure, use your current [public IP address](http://www.myipaddress.com/show-my-ip-address/) with **/32** at the end (for example, **1.2.3.4/32**).
 * IP prefix from which the inbound SSH connections are allowed. If you are not sure, use your current [public IP address](http://www.myipaddress.com/show-my-ip-address/) with **/32** at the end (for example, **1.2.3.4/32**).
* Size of the instance EBS volume (in GB) should be calculated based on the 4% of the expected backup data size. For example, for storing 10 TB of data, the instance EBS volume should be 400 GB.

**Note:** When creating the CloudFormation stack, at the bottom of the **Review** page, select the check box in the **Capabilities** section.

![Capabilities_checkbox](https://cloud.githubusercontent.com/assets/1557544/10256747/2feb29cc-6921-11e5-9d3b-d7974fb5753f.png)

Once you have collected that information, find your target region, note the AMI ID for that region, and click the corresponding **Launch Stack** link.

| Region         | AMI ID        | Simple Stack (default VPC) | VPC Stack (your VPC) |
| -------------- | ------------- | ------------ |---------- |
| us-east-1      | ami-9a5ad28d  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| us-west-1      | ami-a296d7c2  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| us-west-2      | ami-1406ca74  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| eu-west-1      | ami-0f15777c  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| sa-east-1      | ami-53dc4b3f  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=sa-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.sa-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=sa-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.sa-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| eu-central-1      | ami-8ee014e1  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-central-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-central-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| ap-southeast-1 | ami-e29b4481  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| ap-southeast-2      | ami-7984ae1a  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| ap-norhteast-1      | ami-3645bd57  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-northeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-northeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-northeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-northeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|


Once the CloudFormation stack is built, go to its **Outputs** tab at the bottom of the AWS Console. Copy the instance ID (you will need it for logging in to the Enhanced Snapshots), click the URL, and then proceed to [Getting Started](#getting-started).

You should also:

* Select minimum m3.large instance and minimum 8 GB size for the volume.
* Select the **es_admin** role while launching the instance to get access to CloudWatch.

![Role](https://cloud.githubusercontent.com/assets/13747035/11899135/bcaaee2c-a5a5-11e5-965b-78d60d64f3b8.png)

# Getting Started
If you have not followed the [Quick start](#quick-start) section, then you will first need to manually [create an IAM role](#iam-role-creation-optional), and then create an EC2 instance using the Enhanced Snapshots AMI, which can be found in the first table above.

**Note:** By default a new instance has a self-signed SSL certificate, so you need to bypass your browser security warning to start.

*Step 1*

To log in for the first time, use the following credentials:

* Login: **admin@enhancedsnapshots**
* Password:  Your AWS EC2 Instance ID (available on the [AWS console](https://console.aws.amazon.com/ec2))
  ![Login](https://cloud.githubusercontent.com/assets/13731468/21640004/d25ab6a4-d27b-11e6-8d47-07aa686f30f7.png)

*Step 2*

After you log in, you can do the following:

* Edit the **S3 Bucket** name.
The S3 Bucket name must start with **enhancedsnapshots** and meet the general [AWS requirements](http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html) for the bucket names.
**Note:** If you edit the **S3 Bucket** name on this page, you cannot modify it later.
* Edit SDFS settings.
* Enable Sign-on options. For more information on how to configure the single sign-on options, see the [Configuration Management](https://github.com/SungardAS/enhanced-snapshots/blob/master/ConfigurationManagement.md) file.
* Edit Email notifications. For more information on how to edit email notifications, see the [Configuration Management ](https://github.com/SungardAS/enhanced-snapshots/blob/master/ConfigurationManagement.md) file.
  ![Configuration Settings](https://cloud.githubusercontent.com/assets/13731468/21640006/d25b772e-d27b-11e6-9bad-81ec34c1545d.png)

*Step 3*

To create the first user that will automatically receive the administrator privileges, in the **New User** dialog box, specify the necessary information, and then click **Add User**.

**Notes:**

* Email is used as the user ID when logging in to the application.
* Password must not be less than eight characters and include at least one uppercase, one lowercase, one non-alphanumeric character, and one digit (from 0 through 9).
  
  ![New user](https://cloud.githubusercontent.com/assets/13731468/21645351/652038e2-d29a-11e6-8000-e7989e1d4fab.png)

*Step 4*

After you click **Add User**, the system automatically creates an environment, and then redirects you to the new user login page.

To log in as a new user, type the credentials that you specified in step 3.

![Login](https://cloud.githubusercontent.com/assets/13731468/21640007/d25c3556-d27b-11e6-91fc-2f7ee4890e44.png)

After logging in as a new user, a list of the EBS volumes for the local region is displayed.

![Volumes](https://cloud.githubusercontent.com/assets/13731468/21640008/d25e63b2-d27b-11e6-9dff-feb50d655927.png)


# Management Tasks

## Creating Manual Backups
To create a backup:

1. On the **Volumes** tab, select the appropriate EBS volumes, and then click **Backup selected**.
     
     ![Volumes (backup selected)](https://cloud.githubusercontent.com/assets/13731468/21640003/d25a55c4-d27b-11e6-88ae-f5ea4ee6c4b0.png)
     
2. In the **Backup Volume** dialog box, click **Add backup task**.
3. In the confirmation dialog box, click either **Go to Tasks** or **Stay on Volumes**.
     
     **Note:** Only up to five backup tasks can be run in parallel.
 ![multiple backups](https://cloud.githubusercontent.com/assets/13731468/21640017/d28f94d2-d27b-11e6-973d-66b7f7bb8949.png)

To cancel a backup task in process or delete a task from the history:

1. On the **Tasks** tab, hover over the required task, and then click **Reject**.
2. In the confirmation dialog box, click **Reject** or **Delete**.

To delete an old backup manually:

1. On the **Volumes** tab, hover over the appropriate EBS volume, and then click **Backup history**.   
2. In the backup history list, select the appropriate volume backups, and then click **Delete selected**. 
3. In the confirmation dialog box, click **Delete**.

## Restoring backups
To restore the latest backup:
1. On the **Volumes** tab, select the appropriate EBS volumes, and then click **Restore selected**.   
2. In the **Restore Backup** dialog box, edit the availability zone to which the backup will be restored if necessary, and then click **Add restore task**.
 ![multiple restores](https://cloud.githubusercontent.com/assets/13731468/21678985/18a7a422-d34a-11e6-8251-c73c53f7ac86.png)
3. In the confirmation dialog box, click either **Go to Tasks** or **Stay on Volumes**. 
     **Note:** Only up to five recovery tasks can be run in parallel.
     
To restore a backup from history:
1. On the **Volumes** tab, hover over the appropriate EBS volume, and then click **Backup history**.   
2. In the backup history list, hover over the appropriate volume backup, and then click **Restore**. 
3. In the **Restore Backup** dialog box, edit the availability zone to which the backup will be restored if necessary, and then click **Add Restore Task**.
4. In the confirmation dialog box, click either **Go to Tasks** or **Stay on Volumes**. 

To cancel a recovery task in process or delete a task from the history:

1. On the **Tasks** tab, hover over the required task, and then click **Reject**.
2. In the confirmation dialog box, click **Reject** or **Delete**.

## Creating a Schedule
You can automate the process of creating backups by scheduling the backup tasks. Schedules are displayed and stored in the [Cron](https://en.wikipedia.org/wiki/Cron) format. The interval between backups can be from one minute to one year. If necessary, schedules can be disabled.

To schedule a backup task:

1. On the **Volumes** tab, hover over the appropriate volume, click **Schedule**, and then click **New Schedule**.
 ![Schedule button](https://cloud.githubusercontent.com/assets/13731468/21640015/d28f2218-d27b-11e6-95b9-8a9ac933c489.png)
2. In the **New Schedule** dialog box, specify the name, frequency, and time for the schedule, ensure that the **Enabled** check box is selected, and then click **Add Schedule**.
 
![New schedule](https://cloud.githubusercontent.com/assets/13731468/21645352/6521dff8-d29a-11e6-9ca1-7ac4b9631858.png)

To view the scheduled tasks for a specific volume, on the **Volumes** tab, hover over the appropriate volume, and then click **Schedule**.

To edit a schedule:

1.	On the **Volumes** tab, click the appropriate volume.
2.	Hover over the required schedule for the volume, and then click **Edit**.
3.	Make the required modifications, and then click **Save Schedule**.
      ![Edit schedule](https://cloud.githubusercontent.com/assets/13731468/21640009/d2741ffe-d27b-11e6-82f7-291d3d12cc40.png)

To delete a schedule:

1.	On the **Volumes** tab, hover over the appropriate volume, and then click **Schedule**.
2.	Hover over the required schedule for the volume, and then click **Delete**.

## Managing Retention Policies
Using the retention policy, you can automatically delete backups according to their backup size, age, and number of containing files.

To create a retention rule:

1.	On the **Volumes** tab, hover over the appropriate volume, and then click **Retention**.

      **Note:** You can create only one retention rule per each volume that was previously backed up.
      
2.	In the **Edit Retention Rule** dialog box, do the following:
       * To delete a backup when its size exceeds a certain value, click the **Size Limit** tab, and then, in the **backup size exceeds** box, specify the limit.
	* To delete a backup when the number of files in this backup exceeds a certain value, click the **Count Limit** tab, and then, in the **total files count exceeds** box, specify the limit.
	* To delete a backup when it is older than a certain number of days, click the **Days Limit** tab, and then,  in the **backup creation date is over** box, specify the limit.
	
	![Edit retention rule](https://cloud.githubusercontent.com/assets/13731468/21640011/d274de08-d27b-11e6-890c-cb5470e358a6.png)
	
3. Click **Save Rule**.

## Filtering Volumes
You can quickly locate the required volumes by filtering them according to different parameters.

To filter the volumes:

1.	In the top right corner under your user name, click **Filter**.
2.	In the **Filter Volumes** dialog box, specify the appropriate filtering parameters, and then click **Apply filter**.
![Filter](https://cloud.githubusercontent.com/assets/13731468/21640010/d2749f88-d27b-11e6-80c3-71623e84f2b2.png)

## Viewing Logs
You can view and trace the logs for all processes in the Enhanced Snapshots tool on the **Logs** tab. To trace the runtime logs, click **Follow Logs**.

**Note:** Only users with the administrator rights can access the **Logs** tab.

## Managing System Settings
Depending on your requirements and goals, you can configure different system setting in the Enhanced Snapshots tool. For more information, see the [Configuration Management](https://github.com/SungardAS/enhanced-snapshots/blob/master/ConfigurationManagement.md) file.

## Managing System Migration
You can migrate to another EC2 instance without any loss of data by doing the following: 
* Ensure that no backup tasks are running
* Perform a system backup. Go to the **Setting** tab, and then click **Backup now**.
* Remember the created bucket name.
* Uninstall the current system as described in [Removing the Enhanced Snapshots system](#removing-the-enhanced-snapshots-system) but do not remove the S3 bucket.
 ![Delete](https://cloud.githubusercontent.com/assets/13731468/21645353/65221f9a-d29a-11e6-9dd0-0c5ab7b89121.png)
 
* Start a new instance with the same or newer version of Enhanced Snapshots.
* During initial configuration step, in the **S3 Bucket** list, select the S3 bucket from the previous system.

##  Other Management Tasks
To view all active and pending tasks, go to the **Tasks** tab.

To specify the time limit for storing the tasks, go to **Settings**, and then, in the **Tasks History Time Limit** box, type the required value.

To view a list of all users, go to the **Users** tab.

If you have the administrator rights, you can edit information about all users including other Enhanced Snapshots administrators. 

If you are a user without administrative rights, you can edit only your user profile. 

If only one administrator user was created in the system, the administrator rights of this user cannot be revoked.

# Removing the Enhanced Snapshots system
To uninstall Enhanced Snapshots, do the following:

1. On the **Settings** tab, click **Uninstall**.
![Settings](https://cloud.githubusercontent.com/assets/13731468/21640018/d28f9b44-d27b-11e6-943e-68ea58512432.png)
2. In the **System Uninstall** dialog box, do the following, and then click **Uninstall**.
 * Specify whether you want to remove the S3 Bucket.
 * Type your system ID.
   
![System uninstall with remove bucket](https://cloud.githubusercontent.com/assets/13731468/21645355/6524bcc8-d29a-11e6-9a0e-a4fbd47b7113.png)

**Note:** It can take several minutes to delete all resources, especially if you have stored the backup data.

The system continues with the removal of all resources once you enter the EC2 Instance ID for the EC2 instance on which the Enhanced Snapshots tool is running.
The following resources are deleted:
* EC2 Instance
* S3 bucket and all the backup data
* DynamoDB tables

**Note:** After the EC2 instance is deleted, the subscription to the software product from AWS is not removed. For more information, go to [AWS Marketplace Help and FAQ](https://aws.amazon.com/marketplace/help/200799470).

# IAM role creation (optional)
If you create an instance directly from the AMI without using the provided CloudFormation template, you must first create an IAM role with the policy as defined in the [es-admin role cloud formation template](https://github.com/SungardAS/condensation-particles/blob/master/particles/cftemplates/sungardas_enhanced_snapshots_admin_role.template.json).
After the role is created, create and save an API key, which you will need to configure the Enhanced Snapshots tool.

Without a properly configured role, the following error message appears during the configuration:
![DynamoDBAccessDenied](https://cloud.githubusercontent.com/assets/14750068/10131876/08b816c8-65dc-11e5-871e-0f8d5fcdd303.png)

# Logging

Enhances Snapshots uses AWS CloudWatch as logs storage. You can find the logs at the following location:
![Logs](https://cloud.githubusercontent.com/assets/13747035/11899181/fc56c14a-a5a5-11e5-9b26-c764b65bdbd6.png)

# License

For license rights and limitations (Apache 2), see the [LICENSE.md](LICENSE.md) file.

Use of the provided AMIs is covered by a separate [End User License Agreement](https://s3-us-west-2.amazonaws.com/sgaslogo/EULA_Enhanced+Snapshots+for+AWS+2015-10-27.docx).

## Sungard Availability Services | Labs
[![Sungard Availability Services | Labs][labs-image]][labs-github-url]

This project is maintained by the Labs team at [Sungard Availability
Services][sgas-url]

GitHub: [https://sungardas.github.io](https://sungardas.github.io)

Blog:
[http://blog.sungardas.com/CTOLabs/](http://blog.sungardas.com/CTOLabs/)


[sgas-url]: https://sungardas.com
[labs-github-url]: https://sungardas.github.io
[labs-image]: https://raw.githubusercontent.com/SungardAS/repo-assets/master/images/logos/sungardas-labs-logo-small.png
[travis-image]: https://travis-ci.org/SungardAS/enhanced-snapshots.svg?branch=master
[travis-url]: https://travis-ci.org/SungardAS/enhanced-snapshot
[github-image]: https://d25lcipzij17d.cloudfront.net/badge.svg?id=gh&type=6&v=3.0.0&x2=0
[github-url]: https://badge.fury.io/gh/sungardas%2Fenhanced-snapshots