Reference and Thanks to:
http://www.uesp.net/wiki/ESOMod:DAT_File_Format
http://www.uesp.net/wiki/ESOMod:DAT_File_Contents
http://www.uesp.net/wiki/ESOMod:MNF_File_Format

Files are stored within bigger files; each with file names and offsets stored in other files.

We build load, parse, unpack key files, to build a database of files and file names.
Then, we are able to pick a file and use the database to unpack it.

Files are stored in directory esounzip, which is in the same directory as the application.

When building the database:
We unpack files into the same directory as the application.
The database is esozip.sqlite, and is in the same directory as the application.

As of June 17, 2014.
1.  Unable to find the ZOSFT file, for the list of file names, for the eso dat files.
2.  When we unpack and rename the game.dat file, the files have “garbage” data in the beginning of every file.  For example: esoui/app/app.txt has garbage.