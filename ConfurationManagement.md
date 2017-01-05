# Configuration management

Enhanced Snapshots is a flexible system where you can configure different settings depending on your needs and goals.   
You can specify system settings either by using the Enhanced Snapthots UI or configuration file.

* [Properties editable from the UI](#Properties-editable-from-UI)
* [Properties editable in the configuration file](#Properties-editable-in-the-configuration-file)

### Properties editable from the UI
You can configure system settings on the **Settings** tab or during the initialization phase.

During the initialization, you can configure the following settings:

* **S3 Bucket name.** You can set a custom S3 bucket name but it must start with the **enhancedsnapshots.** prefix and should meet the [AWS requirements](http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html). After the system is initialized, you cannot modify the S3 bucket name.
* **SDFS Settings**. The volume size depends on the available RAM. The more free RAM a system has, the bigger the volume size can be set. The minimum volume size is 50 GB.
* **Sign-on options**. To configure single sign-on, do the following:
    1.  Select the **Enable Single Sign On** check box.
    2.  Upload the identity provider metadata and a certificate with a key.
    3.  Specify **Entity ID** and **Administrator Email**.
* **Email Notifications**.

![Configuration Settings](https://cloud.githubusercontent.com/assets/13731468/21640006/d25b772e-d27b-11e6-9bad-81ec34c1545d.png)

After the initialization, you can configure the following properties on the **Settings** tab:

* **SDFS volume size.**
* **SDFS local cache size.** SDFS stores all unique data in the cloud and uses a local writeback cache for the performance purposes. Therefore, the most recently accessed data is cached locally and only the unique chunks of data are stored in the cloud storage provider. Size of the local cache depends on the free storage size and cannot exceed it. Current property can be applied only after the SDFS restart and it can be changed only when no backup or restore tasks are in progress.
* **Temporary Volume Type.** While performing backups, Enhanced Snapshots creates a temporal volume from the original one, copies data from a new volume to the SDFS mount point, and then removes it when the backup process is finished. By default, this volume will be of the **gp2** type. You can change it depending on your purposes. Next volume types are available for the temporal volumes:  **standard**, **io1**, **gp2**. For more information about the AWS volume types, see [EBS Volume Types](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html).
* **Restore Volume Type.** Enhanced Snapshots creates a new volume to restore the backup data. By default, this volume will be of the **gp2** type. You can change it to **standard** or **io1**. For more information about the AWS volume types, see [EBS Volume Types](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html).

* **Maximum Task Queue Size.** Enhanced Snapshots allows you to perform up to five backup and five restore tasks simultaneously. If a number of the added backup tasks exceeds five or a number of the added restore tasks exceeds five, other tasks are added to the queue. By default, the queue can contain 20 tasks.
* **Amazon Retry Count:** In case one of the AWS service returns AmazonServiceException, Enhanced Snapshots will attempt to duplicate a request.
* **Amazon Retry Sleep:** This property defines the timeout before a new attempt to send a request to the AWS service after AmazonServiceException.
* **Store Snapshots**. You can specify whether you want to store an AWS snapshot of the last performed backup.
* **Task History Time Limit**. You can specify how long the tasks will be available in the history on the **Tasks** tab.
* **Logs Limit**. You can specify a number of log lines that are displayed on the **Logs** tab.
* **Email notifications**. You can configure email notifications about different events in the Enhanced Snapshots tool. For more information on this refer to SetupEmailNotification.md. 

![Settings](https://cloud.githubusercontent.com/assets/13731468/21640018/d28f9b44-d27b-11e6-943e-68ea58512432.png)

### Properties editable in the configuration file
The Enhanced Snapshots configuration file is available after the system initialization in **$CATALINA_HOME/conf/enhancedsnapshots.properties**. To apply the changes to the file, restart the system.

You can set the following properties in the configuration file:

* **enhancedsnapshots.retention.cron**. The cron schedule for a retention policy. By default, **00 00 * * ?**.
* **enhancedsnapshots.polling.rate**. Polling rate for the functioning dispatcher to check whether there is a new task in the queue, in milliseconds.
* **enhancedsnapshots.wait.time.before.new.sync**. Waiting time before a new synchronization of Snapshot/Volume local data with the AWS data after some changes with Snapshot/Volume, in seconds.
* **enhancedsnapshots.max.wait.time.to.detach.volume**. Maximum waiting time for a volume to be detached, in seconds.
