import com.alibaba.fastjson.JSON;
import lotus.domino.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 22:24 2018/9/30
 */
public class EnSoftFile extends HttpServlet {

    private final static String fileTempFolder = "temp4upload";



    public static String action;
    public static Session session;
    public static Database mssDb;
    public static View mssView;
    public static Document attachmentDoc;
    public static RichTextItem body;
    public static String saveFullFolderPath;

    public static List<FileItem> fileItemList;

    public static FileResult msg;
    public static PrintWriter out;


    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("EnSoftFile 初始化...");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json;charset=utf-8");

        msg = new FileResult();
        action = request.getParameter("action"); //download /delete/upload
        System.out.println("EnSoftFile Action: "+action);



        ///////////1)初始化附件存放的临时目录
        String rootPath = getCurrentPath();
        saveFullFolderPath =  rootPath + File.separator + fileTempFolder;//保存附件的文件夹（全路径）
        File saveFullFolderPathDir =  new File(saveFullFolderPath);
        if(!saveFullFolderPathDir.exists())//若不存在文件夹则创建
            saveFullFolderPathDir.mkdirs();
        System.out.println("保存附件的文件夹: "+saveFullFolderPath);


        //////////2)获取传过来的参数
        String appDocUNID = null,appMSSDatabase = null,md5Code = null;//md5Code要删除或下载的附件md5,定位附件用
        try {
            if ("download".equals(action)){
                appDocUNID = request.getParameter("unid");
                md5Code = request.getParameter("key"); //md5值，文件保存时前缀用md5值，用来匹配下载的文件
                appMSSDatabase = request.getParameter("mssdb");
                System.out.println("MD5 : "+md5Code);
            }

            if ("upload".equals(action)){

                fileItemList = new ArrayList<FileItem>();
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List items = upload.parseRequest(request);
                Iterator itr = items.iterator();// 枚举方法
                while (itr.hasNext()) {
                    FileItem item = (FileItem) itr.next();
                    if (item.isFormField()) {// 判断是文件还是文本信息
                        if(item.getFieldName()!=null && item.getFieldName().equals("AppDocUNID")){
                            appDocUNID = item.getString("UTF-8");
                        }
                        if(item.getFieldName()!=null && item.getFieldName().equals("AppMSSDatabase")){
                            appMSSDatabase = item.getString("UTF-8");
                        }
                        //System.out.println("表单参数名:" + item.getFieldName() + "，表单参数值:" + item.getString("UTF-8"));
                    } else {
                        if (item.getName() != null && !item.getName().equals("")) {// 判断是否选择了文件
                            System.out.println("将要上传文件名:" + item.getName()+"类型:" + item.getContentType()+"文件名:" + item.getSize());
                            // 此时文件暂存在服务器的内存当中
                            fileItemList.add(item);
                        }
                    }
                }
            }

            if ("delete".equals(action)){
                InputStream is = request.getInputStream();
                String bodyInfo = IOUtils.toString(is, "utf-8");
                System.out.println("入参信息："+bodyInfo);
                //参信息：key=29ceb2e501f28a35ab0df04c795a3a04&appMSSDatabase=webea%2FbbsLib1.nsf&id=282D2F58ED82023848258317003155BF&appDocUNID=08C1423E79606B7448258317002DB44E
                md5Code = bodyInfo.substring(bodyInfo.indexOf("key=")+4,bodyInfo.indexOf("&appMSSDatabase="));
                appMSSDatabase = bodyInfo.substring(bodyInfo.indexOf("&appMSSDatabase=")+16,bodyInfo.indexOf("&id="));
                String attachmentDocUNID = bodyInfo.substring(bodyInfo.indexOf("&id=")+4,bodyInfo.indexOf("&appDocUNID="));
                appDocUNID = bodyInfo.substring(bodyInfo.indexOf("&appDocUNID=")+12);
                appMSSDatabase = URLDecoder.decode(appMSSDatabase, "UTF-8");

                System.out.println("attachmentDocUNID: "+attachmentDocUNID);
                System.out.println("md5: "+md5Code);
            }


            System.out.println("appDocUNID: "+appDocUNID);
            System.out.println("appMSSDatabase: "+appMSSDatabase);


            /////////////3）Domino数据处理
            NotesThread.sinitThread();
            session = NotesFactory.createSession();
            mssDb = session.getDatabase(session.getServerName(),appMSSDatabase);
            mssView = mssDb.getView("AttachmentView");
            attachmentDoc = mssView.getDocumentByKey(appDocUNID);




            if("download".equals(action)){
                fileDownload(md5Code,response);
                return;
            }


            out = response.getWriter();
            if("delete".equals(action)){

                fileDelete(md5Code);
            }

            if("upload".equals(action)){
                fileUpload(appMSSDatabase,appDocUNID);
            }


            ////////////4)返回JSON到前端
            String msgReturn = JSON.toJSON(msg).toString();
            //out.write("{\"name\":\"舞蹈家\"}");
            out.write(msgReturn);
            out.flush();
            out.close();



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                NotesThread.stermThread();
                if (mssDb!=null ) mssDb.recycle();
                if (session!=null) session.recycle();
            } catch (NotesException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *
     * @param md5Code
     */
    private void fileDelete(String md5Code)  throws Exception{
        Vector vvalues = null;
        Vector v = null;
        Item delItem = null;
         try {
             body = (RichTextItem) attachmentDoc.getFirstItem("Body");
             v = body.getEmbeddedObjects();
             int vSize = v.size();
             Enumeration e = v.elements();

             while (e.hasMoreElements()) {
                 EmbeddedObject eo = (EmbeddedObject) e.nextElement();
                 if (eo.getType() == EmbeddedObject.EMBED_ATTACHMENT && eo.getName().indexOf(md5Code) != -1) {
                     eo.remove();
                     vSize -= 1;
                 }
             }

             System.out.println(vSize);
             if (vSize == 0) {
                 attachmentDoc.remove(true);
                 // body.recycle(v);
             } else {
                 //删除其他域里的值
                 //0.2 删除text域里的数据 FileName（被包含在eo.getname里）  SavedName（包含md5code） FileSize（md5code开头） FilePath（包含md5code）
                 delItem = attachmentDoc.getFirstItem("FileSize");
                 vvalues = delItem.getValues();
                 Enumeration values = delItem.getValues().elements();
                 while (values.hasMoreElements()) {
                     Object object = values.nextElement();
                     String delValue = (String) object;
                     System.out.println(delValue);
                     if (delValue.indexOf(md5Code) != -1) {
                         System.out.println("find.......");
                         vvalues.removeElement(object);
                     }
                 }
                 delItem.setValues(vvalues);
                 attachmentDoc.save(true, true);
             }
         }catch (NotesException e){
             e.printStackTrace();
         }finally {
             if (body!=null ) body.recycle();
             if (attachmentDoc!=null ) attachmentDoc.recycle();
             if (delItem!=null ) delItem.recycle();
             if (v!=null ) v.clear();
             if (vvalues!=null ) vvalues.clear();
         }

    }

    /**
     * 上传
     * @param appMSSDatabase
     * @param appDocUNID
     * @throws Exception
     */
    private void fileUpload(String appMSSDatabase ,String appDocUNID) throws Exception{
        Item standardTextItem = null;
        //存放要返回到前端显示的附件s
        Map uploadMap = new HashMap();

        try {
            for (FileItem item : fileItemList) {
                File tempFile = new File(item.getName());// 构造临时对象
                String originName = tempFile.getName();
                String md5Code = DigestUtils.md5Hex(item.getInputStream());
                String savedName = md5Code + "_" + originName;
                File file = new File(saveFullFolderPath + File.separator + savedName);
                item.write(file);// 保存文件在服务器的物理磁盘中

                if (attachmentDoc == null) {
                    //新建
                    attachmentDoc = mssDb.createDocument();
                    attachmentDoc.replaceItemValue("Form", "fAttachment");
                    attachmentDoc.replaceItemValue("CreateDateTime", session.createDateTime(new Date()));
                    attachmentDoc.replaceItemValue("UpdateDateTime", session.createDateTime(new Date()));
                    attachmentDoc.replaceItemValue("CreateUser", "p101010101");
                    attachmentDoc.replaceItemValue("ParentUNID", appDocUNID);
                    attachmentDoc.replaceItemValue("FileSize", md5Code + "_" + String.valueOf(file.length()));

                    body = attachmentDoc.createRichTextItem("Body"); //附件上传
                    body.embedObject(EmbeddedObject.EMBED_ATTACHMENT,
                            null, saveFullFolderPath + File.separator + savedName, savedName);
                } else {
                    //追加
                    standardTextItem = attachmentDoc.getFirstItem("FileSize");
                    standardTextItem.appendToTextList(md5Code + "_" + String.valueOf(file.length()));

                    body = (RichTextItem) attachmentDoc.getFirstItem("Body");
                    body.embedObject(EmbeddedObject.EMBED_ATTACHMENT,
                            null, saveFullFolderPath + File.separator + savedName, savedName);
                }

                uploadMap.put(savedName, file.length());
            }

            if (attachmentDoc != null) {
                attachmentDoc.save(true, true);
            }

            FileResult preview = FileInput.getPreivewSettingsUpload(attachmentDoc.getUniversalID(), uploadMap, appMSSDatabase, appDocUNID);
            msg.setInitialPreview(preview.getInitialPreview());
            msg.setInitialPreviewConfig(preview.getInitialPreviewConfig());
            msg.setFileIds(preview.getFileIds());
            msg.setMssDbPath(appMSSDatabase);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (body!=null ) body.recycle();
            if (attachmentDoc!=null ) attachmentDoc.recycle();
        }

    }


    /**
     *
     * @param md5Code
     * @param response
     */
    private void fileDownload(String md5Code, HttpServletResponse response) {

        InputStream is = null;
        OutputStream os = null;

        try {
            body = (RichTextItem) attachmentDoc.getFirstItem("Body");
            Vector v = body.getEmbeddedObjects();

            Enumeration e = v.elements();
            while (e.hasMoreElements()) {

                EmbeddedObject eo = (EmbeddedObject) e.nextElement();
                if (eo.getType() == EmbeddedObject.EMBED_ATTACHMENT && eo.getName().indexOf(md5Code) != -1) {
                    String savedName = eo.getName();
                    String fileName = savedName.substring(savedName.indexOf("_") + 1);
                    is = eo.getInputStream();
                    int filelength = eo.getFileSize();
                    // 设置输出的格式
                    os = response.getOutputStream();
                    //response.setContentType("application/octet-stream;charset=utf-8");
                    response.setContentType("application/octet-stream");
                    response.setContentLength(filelength);
                    fileName = URLEncoder.encode(fileName, "UTF-8");
                    response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ";filename*=utf8''" + fileName + "");

                    // 循环取出流中的数据
                    byte[] b = new byte[4096];
                    int len;
                    while ((len = is.read(b)) > 0) {
                        os.write(b, 0, len);
                    }
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    /**
     *
     * @return
     */
    public String getCurrentPath()
    {
        File directory = new File ("");
        try
        {
            return directory.getCanonicalPath();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return "";
    }


}
