package lejos.pc.comm;

public class NXTCommand implements NXTProtocol {
	private static NXTCommand singleton = new NXTCommand();
	private NXTComm nxtComm = new NXTCommBluecove();	
	private boolean verifyCommand = false;

    public static final int USB = 0;
    public static final int BLUETOOTH = 1;
	

    public NXTInfo[] search(String name, int protocol) {
		return nxtComm.search(name, protocol);
	}

	public void open(NXTInfo nxt) {
		nxtComm.open(nxt);
	}

	public void setVerify(boolean verify) {
		verifyCommand = verify;
	}

	/**
	 * Small helper method to send request to NXT and return verification result.
	 * @param request
	 * @return
	 */
	private byte sendRequest(byte [] request) {
		byte verify = 0; // default of 0 means success
		if(verifyCommand)
			request[0] = DIRECT_COMMAND_REPLY;
		
		nxtComm.sendData(request);
		if(verifyCommand) {
			byte [] reply = nxtComm.readData();
			verify = reply[2];
		}
		return verify;
	}

	/**
	 * Starts a program already on the NXT.
	 * @param fileName
	 * @return
	 */
	public byte startProgram(String fileName) {
		byte [] request = {DIRECT_COMMAND_NOREPLY, START_PROGRAM};
		request = appendString(request, fileName);
		return sendRequest(request);
	}	

	/**
	 * Opens a file on the NXT for reading. Returns a handle number and file size,
	 * enclosed in a FileInfo object.
	 * 
	 * @param fileName e.g. "Woops.rso"
	 * @return
	 */
	
	// !! Something might be wrong with this because I can't run a program
	// twice in a row after calling this. But other calls work after this.
	public FileInfo openRead(String fileName) {
		byte [] request = {SYSTEM_COMMAND_REPLY, OPEN_READ};
		request = appendString(request, fileName); // No padding required apparently
		nxtComm.sendData(request);
		byte [] reply = nxtComm.readData();
		FileInfo fileInfo = new FileInfo(fileName);
		fileInfo.status = reply[2];
		if(reply.length > 3) { // Check if all data included in reply
			fileInfo.fileHandle = reply[3];
			fileInfo.fileSize = (0xFF & reply[4]) | ((0xFF & reply[5]) << 8)| ((0xFF & reply[6]) << 16)| ((0xFF & reply[7]) << 24);
		}
		return fileInfo;
	}

	/**
	 * Opens a file on the NXT for writing.
	 * UNFINISHED
	 * UNTESTED
	 * @param fileName e.g. "Woops.rso"
	 * @return File Handle number
	 */
	public byte openWrite(String fileName, int size) {
		byte [] command = {SYSTEM_COMMAND_REPLY, OPEN_WRITE};
        byte[] asciiFileName = new byte[fileName.length()];
        for(int i=0;i<fileName.length();i++) asciiFileName[i] = (byte) fileName.charAt(i);
		command = appendBytes(command, asciiFileName);
		byte [] request = new byte[22];
		System.arraycopy(command, 0, request, 0, command.length);
		byte [] fileLength = {(byte)size, (byte)(size>>>8), (byte)(size>>>16), (byte)(size>>>24)};
		request = appendBytes(request, fileLength);
		nxtComm.sendData(request);
		byte [] reply = nxtComm.readData();
		return reply[3]; // The handle number
	}

	/**
	 * Closes an open file.
	 * @param handle File handle number.
	 * @return Error code 0 = success
	 */
	public byte closeFile(byte handle) {
		byte [] request = {SYSTEM_COMMAND_NOREPLY, CLOSE, handle};
		
		// The code below is repitative but can't use sendRequest() because SYSTEM_COMMAND being used
		byte verify = 0; // default of 0 means success
		if(verifyCommand)
			request[0] = SYSTEM_COMMAND_REPLY;
		nxtComm.sendData(request);
		if(verifyCommand) {
			byte [] reply = nxtComm.readData();
			verify = reply[2];
		}
				
		return verify;
	}
	
	public byte delete(String fileName) {
		
		byte [] request = {SYSTEM_COMMAND_REPLY, DELETE};
		// !! Below, could use String concat function (str1 + str2)
		request = appendString(request, fileName);
		//request = (new String(request) + fileName).getBytes();
		
		// !! Below should be a method shared by System Commands and Direct Commands.
		nxtComm.sendData(request);
		byte [] reply = nxtComm.readData();
		return reply[2];
	}

	/**
	 * When no files exist within the system, an error message is returned in
	 * the package saying "File not found".
	 * When this command returns a success, a close command is required for "closing
	 * the handle" within the brick when handle is not needed anymore. If an error
	 * is returned, the firmware will close the handle automatically.
	 * @param wildCard [filename].[extension], *.[extension], [filename].*, *.*
	 * @return
	 */
	public FileInfo findFirst(String wildCard) {

		byte [] request = {SYSTEM_COMMAND_REPLY, FIND_FIRST};
		// !! Below, could use String concat function (str1 + str2)
		request = appendString(request, wildCard);
		//request = (new String(request) + fileName).getBytes();
		
		// !! Below should be a method shared by System Commands and Direct Commands.
		nxtComm.sendData(request);
		byte [] reply = nxtComm.readData();
		FileInfo fileInfo = null;
		if(reply[2] == 0) {
			fileInfo = new FileInfo("");
			fileInfo.status = reply[2];
			if(reply.length > 3) { // Check if all data included in reply
				fileInfo.fileHandle = reply[3];
				StringBuffer name= new StringBuffer(new String(reply)).delete(24,27).delete(0, 4);
				int lastPos = name.indexOf(".") + 4; // find . in filename, index of last char.
				name.delete(lastPos, name.length());
				fileInfo.fileName = name.toString();
				fileInfo.fileSize = (0xFF & reply[24]) | ((0xFF & reply[25]) << 8)| ((0xFF & reply[26]) << 16)| ((0xFF & reply[27]) << 24);
			}
		}
		return fileInfo;
	}
	
	/**
	 * When no files exist within the system, an error message is returned in
	 * the package saying "File not found".
	 * When this command returns a success, a close command is required for "closing
	 * the handle" within the brick when handle is not needed anymore. If an error
	 * is returned, the firmware will close the handle automatically.
	 * @param handle Handle number from the previous found file or fromthe Find First command.
	 * @return
	 */
	public FileInfo findNext(byte handle) {

		byte [] request = {SYSTEM_COMMAND_REPLY, FIND_NEXT, handle};
		
		// !! Below should be a method shared by System Commands and Direct Commands.
		nxtComm.sendData(request);
		byte [] reply = nxtComm.readData();
		FileInfo fileInfo = null;
		if(reply[2] == 0) {
			fileInfo = new FileInfo("");
			fileInfo.status = reply[2];
			if(reply.length > 3) { // Check if all data included in reply
				fileInfo.fileHandle = reply[3];
				StringBuffer name= new StringBuffer(new String(reply)).delete(24,27).delete(0, 4);
				int lastPos = name.indexOf(".") + 4; // find . in filename, index of last char.
				name.delete(lastPos, name.length());
				fileInfo.fileName = name.toString();
				fileInfo.fileSize = (0xFF & reply[24]) | ((0xFF & reply[25]) << 8)| ((0xFF & reply[26]) << 16)| ((0xFF & reply[27]) << 24);
			}
		}
		return fileInfo;
	}

	/**
	 * Helper code to append a string and null terminator at the end of a command request.
	 * Should use String.concat if I could add a zero to end somehow.
	 * @param command
	 * @param str
	 * @return
	 */
	private byte[] appendString(byte [] command, String str) {
		String requestStr = new String(command);
		StringBuffer buff = new StringBuffer(requestStr);
		buff.append(str);
		// buff.append(0x00); // Need to add 0 to end of command array.
		buff.setLength(buff.length() + 1); // This is a hack to add null value to end
		return buff.toString().getBytes();
	}

	private byte[] appendBytes(byte [] array1, byte [] array2) {
		byte [] array = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, array, 0, array1.length);
		System.arraycopy(array2, 0, array, array1.length, array2.length);
		return array;
	}

	public int getBatteryLevel() {
		byte [] request = {DIRECT_COMMAND_REPLY, GET_BATTERY_LEVEL};
		nxtComm.sendData(request);
		byte [] reply = nxtComm.readData();
		if(reply[1] != GET_BATTERY_LEVEL)
			System.out.println("Weird data reply received.");
		if(reply[2] != 0)
			System.out.println("NXT reports the check battery command did not work.");
		int batteryLevel = (0xFF & reply[3]) | ((0xFF & reply[4]) << 8);
		return batteryLevel;
	}

	/**
	 * Call the close() command when your program ends, otherwise you
	 * will have to turn the NXT brick off/on before you run another
	 * program using iCommand.
	 *
	 */
	public void close() {
		nxtComm.close();
	}

	public byte writeFile(byte handle, byte [] data) {
		byte [] request = new byte[data.length + 3];
		byte [] command = {SYSTEM_COMMAND_NOREPLY, WRITE, handle};
		System.arraycopy(command, 0, request, 0, command.length);
		System.arraycopy(data, 0, request, 3, data.length);
				
//		 The code below is repetitive but can't use sendRequest() because SYSTEM_COMMAND being used
		byte verify = 0; // default of 0 means success
		if(verifyCommand)
			request[0] = SYSTEM_COMMAND_REPLY;
		nxtComm.sendData(request);
		if(verifyCommand) {
			byte [] reply = nxtComm.readData();
			verify = reply[2];
			// Next line can be used to confirm if data written:
			// int bytesWritten = (0xFF & reply[4]) | ((0xFF & reply[5]) << 8);
		}
		
		return verify;
	}

	/**
	 * Returns requested number of bytes from a file. File must first be opened
	 * using the openRead() command.
	 * @param handle File handle number (from openRead method)
	 * @param length Number of bytes to read.
	 * @return
	 */
	public byte [] readFile(byte handle, int length) {
		byte [] request = {SYSTEM_COMMAND_REPLY, READ, handle, (byte)length, (byte)(length>>>8)};
		nxtComm.sendData(request);
		byte [] reply = nxtComm.readData();
		byte [] data = new byte[reply.length - 6];
		// Copy data into array:
		// !! * Could crash due to receiving no data array. Test with bad handle.
		System.arraycopy(reply, 6, data, 0, data.length);
		return data; // The handle number
	}

	public static NXTCommand getSingleton() {
		return singleton;
	}
}


