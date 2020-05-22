# XFM2_GetterSetter
small JavaFX application to read/write &amp; text edit XFM2 json patches, see also:

- https://github.com/xerhard/XFM2_patches
- https://github.com/xerhard/DX7syx-to-XFM2patches


Application was made for personal usage. I did not code in Java for > 15 years and code is against all coding conventions, but it works (kind of, at least for me ;) ).

Serial device path is hard coded, but should work on Linux, MacOS and Windows, see code

# GUI

- Buttons speak for them selves
- First textfield: Preset number
- Second textfield: Json data, can be edited
- Bottom textfield: system messages

![GUI](https://github.com/xerhard/XFM2_GetterSetter/blob/master/images/Screenshot_XFM2_GetterSetter.png "UI XFM2_GetterSetter")

### TODO

Funny characters like "\\" in DX7 presetnames can produce errors when sending patch to XFM2 buffer.

