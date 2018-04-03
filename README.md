# gv-adapter-qrcode
### GV ESB v4 adapter for QR code handling

Include`qr-encode-call` to encode an arbitrary string in QR Code.

#### Required attributes
 - **name**
 - **type** (=call)
 - **size** _for sqare QR code or alternatively_ **width** _and_ **height**

#### Optional attributes 
 - **bgColorCode** _background ARGB color code (default #FFFFFFFF)_
 - **fgColorCode** _foreground ARGB color code (default #FF000000)_
 - **logo** _local (file system) or remote (http) URI of a overlay image to place centered over QR code_
