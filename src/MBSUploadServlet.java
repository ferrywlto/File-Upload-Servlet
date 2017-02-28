

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import vg.common.appConfig.ApplicationField;

import flex.messaging.FlexSession;

public class MBSUploadServlet extends HttpServlet {
	
	private static final String UPLOAD_DIRECTORY = Initializer.getConfiguration().get(ApplicationField.UPLOAD_FOLDER_REAL);
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		PrintWriter pw;
		try {
			String sessionID = req.getParameter("s");
			pw = res.getWriter();
			pw.println("your session ID: "+ sessionID);
			pw.println("all existing session:");
//			Enumeration<String> keys = allSession.keys();
//			while(keys.hasMoreElements()){
//				pw.println(keys.nextElement());
//			}
			pw.close();
		} catch (IOException e) { MBSLogger.logError(e); }
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) {

		boolean sessionValid = false, canUpload = false, uploadSuccess = false, thumbnailSuccess = false, dbSuccess = true;
		
		String sessionID = "";
		// two passes
		// 1. get HTTP form field first
		try {
			List<FileItem> items = Initializer.getUploadRequestParser().parseRequest(req);
			Iterator<FileItem> itemIterator = items.iterator();
			while(itemIterator.hasNext()) {
				FileItem item = itemIterator.next();
				if(item.isFormField()) { 
					//HTML form field... should not encounter this one
					MBSLogger.log("[UPLOAD] field detected:"+item.getFieldName()+"#"+item.getString());
					if(item.getFieldName().equals("sessionID")) {
						sessionID = item.getString();
						MBSLogger.log("sessionID?:"+sessionID);
						//TODO: check for permission either (later)
						if(AMFPortal.isSessionValid(sessionID)){
							//Initializer.getAllFlexSession().get(sessionID).getAttribute("user");
							canUpload = true;
							//need: userID, contentID, content info before write to disk
							//for upload path
							//
						}
						//Initializer.getDatabaseConnector().
					} 
				}
			}
			MBSLogger.log("sessionValid?:"+canUpload);
			if(canUpload) {
				// 2nd Pass, write the stream to file once if upload is valid
				itemIterator = items.iterator();
				while(itemIterator.hasNext()) {
					FileItem item = itemIterator.next();
					if(!item.isFormField()) { 

						MBSLogger.log(fileInformation(item));
						try {
							item.write(new File(UPLOAD_DIRECTORY + item.getName()));
							uploadSuccess = true;
						} catch(Exception e) {
							MBSLogger.logError(e);
							uploadSuccess = false;
						}
						thumbnailSuccess = (generateThumbnail(UPLOAD_DIRECTORY + item.getName()) == ReturnCode.NO_ERROR.value);
						
						MBSLogger.log("uploadSuccess:"+uploadSuccess+"|thumbnailSuccess:"+thumbnailSuccess);
						
						outputMessage(res, (uploadSuccess && thumbnailSuccess && dbSuccess)?"success":"error");
					}
				}
			} else {
				outputMessage(res, "Error: Not Authenticated.");
			}
		}
		catch (Exception e) { MBSLogger.logError(e); } 
	}
	
	protected int generateThumbnail(String inputFile){
		Process proc = ThumbnailGenerationTaskDispatcher.getInstance().dispatchTask("sqcif",
				inputFile,
				UPLOAD_DIRECTORY + "thumbnail_small_%01d.jpg");
		
		String tmpStr = "", consoleOutput = "";
		
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		try {
			while((tmpStr = br.readLine()) != null)
				consoleOutput += tmpStr;
			MBSLogger.log(consoleOutput);
			return proc.waitFor();
			
		} catch (Exception e) {
			MBSLogger.logError(e);
			return ReturnCode.ERROR_DEFAULT.value;
		}
	}
	
	protected String fileInformation(FileItem item) {
		String msg = "File detected:\n";
		msg += "fieldName:"+item.getFieldName()+"\n";
		msg += "filename:"+item.getName()+"\n";
		msg += "contenttype:"+item.getContentType()+"\n"; //upload file name
		msg += "inMemory:"+item.isInMemory()+"\n";
		msg += "size:"+item.getSize()+"\n";
		msg += "saved at:" + UPLOAD_DIRECTORY + item.getName()+"\n";
		return msg;
	}
	
	protected void outputMessage(HttpServletResponse res, String msg) {
		PrintWriter pw;
		try {
			pw = res.getWriter();
			pw.write(msg);
			pw.close();
		} catch (IOException e) { MBSLogger.logError(e); }
	}
}
