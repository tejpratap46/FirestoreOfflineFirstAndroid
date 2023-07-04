![](https://img.shields.io/badge/Build-WORK_IN_PROGRESS-red)

# Firestore Offline First
A versatile library that helps you create a Solid Offline First experience but also lets your users to sync data between multiple devices.

## Why?
Firestore already has a decent offline capabilities, it caches all data in a SQLite DB to serve it without hitting server or without increasing read cost.

If you are offline and enabled persistence (`Firebase.database.setPersistenceEnabled(true)`), your data will be proxies through local SQLite and synced when Network connection is available.

All of your offline saves are first saved in Firestore's SQLite db and once network is available, it sends it to Google Servers.


So, Question arrives "Why use this library".
A. If your app is an 'Offline First' means it maintains a database (SQLite, Realm or any other DB) and only need firestore to sync data across devices then when your user is switching devices you Firestore SQLite does not hel, it will download all the data from server to complete restore on second device.

#### For Example:
- Let's say you are an Task Manager App, you store/manage all user's data in your SQLite/Realm, Now you need to enable sync for your users. You see firestore as a Good option and now you implement it with with Firestore + Firebase Auth.
- You need user(all) data on the device locally available to be able to serve your user completely offline.
- Now your user wants to login to second device, you can either:
  - Download all data from firestore and build your local DB from scratch and probably exploding your Daily Quota.
  - Or you download a backup of user's DB[SQLite/Realm] from a backup[Firebase Storage/AWS S3/Google Drive] and continue firestore sync from that point and save Read Cost.

##### Lets see what it takes to implement second way of syncing
1. Create backup of your local DB in S3 Or Firebase Storage Or user's Google Drive.
2. Restore DB after login.
3. Know till where that DB has data and read remaining of data from firestore.
4. Now, only new data should be written or read from firestore to keep devices in sync.

## Setup
#### Create Firestore Indexes
1. This library uses collection`userDevices` to tracks user's devices, add one index for this collection
```json
{
  "collectionName": "userDevices",
  "fields": [
    {
      "name": "userId",
      "sortOrder":  "Ascending"
    },
    {
      "name": "uploadedAt",
      "sortOrder":  "Ascending"
    }
  ],
  "queryScope": "Collection"
}
```
2. Now you have to add Indexes for every other collection you use
```json
{
  "collectionName": "userDevices",
  "fields": [
    {
      "name": "userId",
      "sortOrder":  "Ascending"
    },
    {
      "name": "updatedBy",
      "sortOrder":  "Ascending"
    },
    {
      "name": "uploadedAt",
      "sortOrder":  "Ascending"
    }
  ],
  "queryScope": "Collection"
}
```