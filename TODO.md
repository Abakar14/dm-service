# Open futures for DM service

## TODO

### Open

- [ ] make UploadFileResultResponse return following
  {
  "uploadId": "abc-123",
  "ownerId": 10,
  "domainType": "VEHICLE",
  "uploadType": "VEHICLE_PHOTO",
  "renditions": [
  {
  "renditionType": "ORIGINAL",
  "url": "/api/v1/public/media/abc-123/original"
  },
  {
  "renditionType": "THUMBNAIL",
  "url": "/api/v1/public/media/abc-123/thumbnail"
  },
  {
  "renditionType": "PREVIEW",
  "url": "/api/v1/public/media/abc-123/preview"
  }
  ]
  }
- [ ] minimalize image or document size before save it. to reduce disk space
- [ ] Sometimes, images in the database are deleted from the local hard disk. When searching for the
  file name, an error message stating that the file could not be found appears, and the search for
  the next file stops.
- [ ] change folder structure with update in database-services side
- [ ] darf nicht zwei version in einer besitzter
- [ ] We need Response Entity with info about the file like path, filename
- [ ] save Object DocumentResponse in Database
- [ ] Bei download Request we take the filename and look for their path and return the file as
  response
- [ ] use pages for all endpoints with a mount of data
- [ ] create some interface to take a picture with laptop Camera or usb Camera
- [ ] On upload change the profile picture and resize the size of it.

### In Progress

- [ ] update Readme.md
- [ ] Create Dockerfile
- [ ] create a security library to use for JWT token
- [ ] update endpoints
- [ ] add archive endpoints
- [ ] add delete endpoints

### Done ✓

- [x] added eureka Client
- [x] added to gateway
- [x] Test from postman
- [x] added download by filename
- [x] add Document Specification
- [x] add swagger sort to properties file
- [x] add endpoint getAllDocuements /documents with pages
- [x] add endpoint getAllDocuments /owner/id list
- [x] add endpoint getAllDocuments /owners/list of owners with pages
- [x] add endpoint /upload (Normal one file to the first level hierarchic )
- [x] add endpoint /uploads (here you can send different many files to same folder middle complex
  hierarchic)
- [x] add endpoint /uploads/properties (very Special endpoint very complex hierarchic) 

