# SimpleFrame

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="75" alt="Link to F-Droid download">](https://fdroid.gitlab.io/artwork/badge/get-it-on.png)

Displays a slideshow of images to transform older devices into picture frames.

There are no networking features since old devices are inherently insecure. I recommend transfering the images using a USB connection.

## Photo frame setup

1. Open **Settings** and choose a picture folder. Every supported image in that
   folder and its subfolders is included. The folder picker starts at the
   storage root so internal storage, SD cards and USB drives are visible.
2. Choose **Brightness control**:
   - **Use system setting** leaves Android's current brightness unchanged.
   - **Manual** enables the brightness slider.
   - **Automatic** uses the device's ambient-light sensor.
3. Optionally enable **Night schedule** and select daily screen-off and
   screen-on times. Grant **Modify system settings** when prompted so
   SimpleFrame can change automatic brightness and fully turn the display off.
4. To use a dedicated device as a frame, select SimpleFrame as Android's
   **Home app**. It will launch after boot and wait for removable storage to
   finish mounting before starting the slideshow.

## Screenshots

<img src="metadata/en-US/images/phoneScreenshots/1.png" width="300" alt="Stack transition" title="Stack transition">
<br/><br/><br/>

<img src="metadata/en-US/images/phoneScreenshots/2.png" height="300" alt="Fade transition" title="Fade transition">
<br/><br/><br/>

<img src="metadata/en-US/images/phoneScreenshots/3.png" height="300" alt="Accordion transition" title="Accordion transition">
<br/><br/><br/>

<img src="metadata/en-US/images/phoneScreenshots/4.png" height="300" alt="Stack transition landscape" title="Stack transition landscape">
<br/><br/><br/>

<img src="metadata/en-US/images/phoneScreenshots/5.png" width="300" alt="Settings" title="Settings">
<br/><br/><br/>

### License

Copyright (C) 2024 Louis-Simon Mc Nicoll.

SimpleFrame is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SimpleFrame is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SimpleFrame.  If not, see <http://www.gnu.org/licenses/>.

See also: [LICENSE](LICENSE)

#### Forked from PicFrame at https://github.com/PicFrame/picframe

##### Picframe license

Copyright (C) 2015 Martin Bayerl, Myra Fuchs, Clemens Hlawacek, Christoph Krasa, Linda Spindler, Ebenezer Bonney Ussher.

PicFrame is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

PicFrame is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with PicFrame.  If not, see <http://www.gnu.org/licenses/>.

See also: [LICENSE](LICENSE)
