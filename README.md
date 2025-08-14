![LUY Viewer logo](LUYViewer_logo.png)

Permits LUY users to download data and view it offline / without LUY.


## Installation

1) Go to the [releases](https://github.com/bjoernwuest/LUYViewer-Java/releases) and download the package appropriate for your environment.
2) Unpack the downloaded package.
3) Edit "config.json" file, particularly set the URL to your "luy_host".
4) Run the LUYViewer application (on Windows, this is LUYViewer.exe or LUYViewer.bat)


## Uninstallation

Delete the unpacked package.


## Usage

This section describes step-by-step how to use the LUYViewer application. It is split into the sections "Before first use", "Daily Use", and "Cleanup".

### Some words of warning before use

* The LUYViewer is just a viewer for LUY data. It never manipulates any data in LUY.
* The LUYViewer stores the data viewed locally in the sub-folder "data". The data is not encrypted or otherwise protected. So it is up to you to ensure that the data is not accessible to someone else.
* The LUYViewer expects local, single-user usage. Thus, there is no login, authentication, or other mean of security!
* As I expect that most people use the LUYViewer in business context, your IT department may have locked down your working environment in a way that execution is not permitted. In Windows, this usually involves the Windows Privacy Shield, and sometimes network configuration preventing actual download from your LUY host. If unsure, feel free to ask your IT department to check this [Github repository](https://github.com/bjoernwuest/LUYViewer-Java) for any helpful information and at last resort, reach out to me.

### Before first use

While very simple, the application requires basic configuration before use. The whole configuration is held in a file called "config.jsonc". It follows the [JSON specification](https://www.json.org/). The following configuration options are available:

| Configuration | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Example |
|--|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--|
| luy-host | The URL of the LUY. If it is LUY in the cloud, this usually looks like https://your_name.luy.app . If you operate LUY on premises, this may look different. Important is that you start with http:// or https:// - whatever is applicable in your setup.<br>Furthermore, make sure that you do not end the URL with a slash. If your LUY uses a different port than the standard web port (80 or 443), you may specify the port at the end in the format :Port . | https://demo.luy.app<br>https://my.luy.biz:8123
| language | Here you define the language the LUYViewer shall be (not the data displayed!). It is a two letter code. As of July 4th 2025, only de for German and en for English are supported. | en |

### Daily use

TODO

Whatever you do, keep in mind that using the LUYViewer you do not modify data in LUY.


### Cleanup

The LUYViewer keeps snapshots of the LUY data in the local data folder. Depending on the size of your model, this can be a few megabytes per file. While today this may not harm your disk, it has two impacts:

1. The selection list for the data set to view may become very long if you have a lot of snapshots. While this is not a performance impact, it makes it more difficult to select the proper data set.
1. Any data on your local device may be a potential risk, e.g. if you loose the device. Thus, have only as less data as necessary is a good security practice.

How to cleanup? Simple. LUYViewer stores two files for each snapshot of LUY data: the metamodel and the actual data. Each snapshot is preceeed by the timestamp when the snapshot was taken in the format of Year-Month-Day Hour-Minute-Second_, and then followed by metamodel for the LUY metamodel, and data for the actual LUY Data. To delete a snapshot, simply delete both files with the same timestamp. That's it.

## Privacy, data security, and code signing policy

This program will not transfer any information to other networked systems unless specifically requested by the user or the person installing or operating it.

Data remains local with the LUYViewer, whereever it is run. The LUYViewer is just a reader, it does not modify any data in your LUY instance. All data stored with the LUYViewer, i.e. downloaded snapshots of your data in LUY, is **NOT** encrypted. It is up to you to keep your data with the required level of security and privacy.


## License

The LUYViewer is licensed under the [Apache2 license](LICENSE).


## Funny and motivational

The initial variant of this software was written in Typescript on Deno. It was an 8-hour effort, experimenting with the coding power of Claude AI. This went astonishingly well. Yet, the resulting application was a "web application", meaning it would open a local port - something "very bad" in business context. Thus, I decided to do a rewrite in Java. First, using good old Swing but experiencing too many limitations in data layout, thus experimenting with the "new" JavaFX. At least to me, JavaFX is new since I stopped serious coding back in 2004 .

The motivation of this application is the increasing effort of the LUY company to offer their product LUY as cloud-only SaaS solution. While comfortable and convenient, you are out of control of your data within LUY to some extent (in my opinion). This is critical especially since LUY is often used to document process and IT landscapes, and thus likely used in Business Continuity Management, Emergency Response Teams, etc.. My background as consultant for iteratec GmbH, the initial developer of LUY (formerly iteraplan) from 2011 to 2016/2019, gives me the necessary background in understanding how LUY is used, as well as what is required in an emergency case where LUY is unavailable - for a couple of hours or a few days.


## Call for support

For Windows, it would help if the application is signed. Unfortunately, there are very few options for free-of-charge open source software, and those focus on more widespread software also discussed in mainstream media. Since the LUYViewer does not fit into this category, I would appreciate any support in getting the LUYViewer signed for Windows. If you want to support (e.g. because you have signing facility, or you are willing to pay for commercial signing), please reach out to me.
