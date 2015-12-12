# videoSearch

Video Search with UI 

Can create database and query using small video frames with sound. 

=========database==========================

For the database, each categories must be created like "movie",follow the same naming form(same video/audio naming form) and put in the same folder with "searchvideo.py". Each category has one 20 seconds audio/video file.

Each audio/video file includes 600 frames and 1 wav as corresponding audio information. Each frame is in rgb format with width 352 and height 288. 

"save.json" is a created database based on 7 categories for testing purpose

database categories: ["flowers","interview","movie","musicvideo","sports","starcraft","traffic"]

=========query==============================

For current query database, we have two 5 seconds audio/video file.

Each audio/video file includes 150 frames and 1 wav as corresponding audio information. Each frame is in rgb format with width 352 and height 288. 

query folder must be put in the "query/" , like "interview", and follow the same naming form


============Command================

"searchvideo.py": create database(or using existing one: "save.json") and then query "interview"

$ python searchvideo.py interview001.rbg interview.wav 

"frmMainPage.java": video search UI 

then open files folder "query" to see search result





