import com.alibaba.fastjson.JSON;
import lotus.domino.*;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 19:04 2018/8/18
 */
public class EnUploader extends HttpServlet  {


    //previewFileIconSettings
    public static Map fileIconMap=new HashMap();


    static {
        fileIconMap.put("doc" ,"<i class='fa fa-file-word-o text-primary'></i>");
        fileIconMap.put("docx","<i class='fa fa-file-word-o text-primary'></i>");
        fileIconMap.put("xls" ,"<i class='fa fa-file-excel-o text-success'></i>");
        fileIconMap.put("xlsx","<i class='fa fa-file-excel-o text-success'></i>");
        fileIconMap.put("ppt" ,"<i class='fa fa-file-powerpoint-o text-danger'></i>");
        fileIconMap.put("pptx","<i class='fa fa-file-powerpoint-o text-danger'></i>");
        fileIconMap.put("jpg" ,"<i class='fa fa-file-photo-o text-warning'></i>");
        fileIconMap.put("pdf" ,"<i class='fa fa-file-pdf-o text-danger'></i>");
        fileIconMap.put("zip" ,"<i class='fa fa-file-archive-o text-muted'></i>");
        fileIconMap.put("rar" ,"<i class='fa fa-file-archive-o text-muted'></i>");
        fileIconMap.put("default" ,"<i class='fa fa-file-o'></i>");
    }


    private static String saveFilesFolder = "temp4upload";
    //private ServletContext sc;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
       // savePath = servletConfig.getInitParameter("savePath");
        System.out.println("EnUploader 初始化...");
        //sc = servletConfig.getServletContext();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //super.doGet(httpServletRequest, httpServletResponse);
        System.out.println("EnUploader doGet running...");
        doPost(request, response);

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("JVM totalMemory:"+Runtime.getRuntime().totalMemory());
        System.out.println("JVM freeMemory:"+Runtime.getRuntime().freeMemory());
        System.out.println("EnUploader doPost running...");

        String rootPath = getCurrentPath();
        //保存附件的文件夹（全路径）
        String saveFullFolderPath =  rootPath + File.separator + saveFilesFolder;
        File saveFullFolderPathDir =  new File(saveFullFolderPath);
        //若不存在文件夹则创建
        if(!saveFullFolderPathDir.exists())
            saveFullFolderPathDir.mkdirs();

        System.out.println("savefullfolderpath: "+saveFullFolderPath);


        String action = request.getParameter("action");
        System.out.println("action:"+action);

        //业务文档UNID及其海量库路径
        String appDocUNID= null,appMSSDatabase = null;
        Database mssDb=null;
        Session session=null;

        if (action!=null & action.equals("del")){
            String attDocUnid = request.getParameter("id");
        }


        /*1）下载*/
        if (action!=null & action.equals("download")){

            InputStream is = null;
            OutputStream os = null;
            File file = null;
            Document attDoc =null;
            RichTextItem body = null;
            String attdocUnid = request.getParameter("unid");
            String fileName = request.getParameter("filename");
            appMSSDatabase = request.getParameter("mssdb");

            // 下载网络文件
            int bytesum = 0;
            int byteread = 0;


            try {
                NotesThread.sinitThread();
                session = NotesFactory.createSession();
                mssDb = session.getDatabase(session.getServerName(), appMSSDatabase);
                attDoc = mssDb.getDocumentByUNID(attdocUnid);
                if (attDoc != null){
                    body = (RichTextItem) attDoc.getFirstItem("Body");
                    Vector v = body.getEmbeddedObjects();
                    Enumeration e = v.elements();
                    while (e.hasMoreElements()) {
                        EmbeddedObject eo = (EmbeddedObject)e.nextElement();
                        if (eo.getType() == EmbeddedObject.EMBED_ATTACHMENT) {
                            String attachmentForDownload = saveFullFolderPath + File.separator + eo.getSource();
                            System.out.println(attachmentForDownload);
                            eo.extractFile(attachmentForDownload);
                            eo.remove();

                            if(attachmentForDownload != null & !attachmentForDownload.equals(""))
                                file = new File(attachmentForDownload);
                            if (file != null && file.exists() && file.isFile()) {
                                fileName = file.getName();
                                //System.out.println("将要下载的文件名1"+fileName);
                                //fileName = URLEncoder.encode(fileName, "utf-8");
                                //System.out.println("将要下载的文件名2"+fileName);
                                //有中文文件名乱码的问题
                                long filelength = file.length();
                                is = new FileInputStream(file);
                                // 设置输出的格式
                                os = response.getOutputStream();
                                //response.setContentType("application/octet-stream;charset=utf-8");
                                response.setContentType("application/octet-stream");
                                response.setContentLength((int) filelength);
                                fileName = URLEncoder.encode(fileName, "UTF-8");
                                //String $encoded_fname = new String(fileName.getBytes("UTF-8"),"ISO8859_1");
                                //response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ";filename*=utf8''" + fileName+ ""  );
                                response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ";filename*=utf8''" + fileName+ ""  );

                                //response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes("GB2312"),"ISO8859-1"));
                                // 循环取出流中的数据
                                byte[] b = new byte[4096];
                                int len;
                                while ((len = is.read(b)) > 0) {
                                    os.write(b, 0, len);
                                }




                            } else {
                                response.setContentType("text/html;charset=utf-8");
                                response.getWriter().println("<script>");
                                response.getWriter().println(" modals.info('文件不存在!');");
                                response.getWriter().println("</script>");
                            }

                        }
                    }

                } else {
                    response.setContentType("text/html;charset=utf-8");
                    response.getWriter().println("<script>");
                    response.getWriter().println(" modals.info('文件不存在!');");
                    response.getWriter().println("</script>");
                }

            }catch  (Exception e){
                e.printStackTrace();
            }finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }

                try {
                    NotesThread.stermThread();
                    if (mssDb!=null ) mssDb.recycle();
                    if (session!=null) session.recycle();
                    if (attDoc!=null ) attDoc.recycle();
                    if (body!=null ) body.recycle();

                } catch (NotesException e) {
                    e.printStackTrace();
                }

            }
            return;
        }


        /*2）上传*/
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        FileResult msg = new FileResult();
        //request过来的附件集合
        List<FileItem> fileItemList = new ArrayList<FileItem>();
        //保存后的documentList
        //List<Document> fileDocList = new ArrayList<Document>();
        //失败时：存放数据
        ArrayList<Integer> arr = new ArrayList<Integer>();
        //response.setContentType("application/json");

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        // Configure a repository (to ensure a secure temp location is used)
        //ServletContext servletContext = this.getServletConfig().getServletContext();
        //服务器根路径 /local/notesdata03

        //缓存当前的文件
        List<SysFile> fileList=new ArrayList<SysFile>();

        //保存文件的前缀
        String filePrefixFormat="yyyyMMddHHmmssS";

        try {
            List items = upload.parseRequest(request);// 上传文件解析
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
                        System.out.println("上传文件的大小:" + item.getSize());
                        System.out.println("上传文件的类型:" + item.getContentType());
                        // item.getName()返回上传文件在客户端的完整路径名称
                        System.out.println("上传文件的名称:" + item.getName());
                        // 此时文件暂存在服务器的内存当中
                        fileItemList.add(item);

                    } else {

                    }
                }
            }


        } catch (Exception e) {

            e.printStackTrace();
        }finally {

        }


        System.out.println("主表单UNID:" + appDocUNID + "，海量库路径:" + appMSSDatabase);
        Document attDoc = null;
        RichTextItem body = null;
        Item standardTextItem = null;
        View mssView = null;
        /**
         * 初始化
         * 海量库
         * 并
         * 遍历附件
         */
        try {
            NotesThread.sinitThread();
            session = NotesFactory.createSession();
            mssDb = session.getDatabase(session.getServerName(), appMSSDatabase);
            mssView = mssDb.getView("AttachmentView");

            System.out.println("遍历附件并保存到海量库...");
            System.out.println(fileItemList.size());

            attDoc = mssView.getDocumentByKey(appDocUNID);
            for(FileItem item : fileItemList){

                File tempFile = new File(item.getName());// 构造临时对象
                String originName = tempFile.getName();
                String filePrefix = DateUtil.format(new Date(),filePrefixFormat);
                String savedName = filePrefix + originName;
                File file = new File(saveFullFolderPath + File.separator  + savedName);
                item.write(file);// 保存文件在服务器的物理磁盘中

                if (attDoc == null) {
                    attDoc = mssDb.createDocument();
                    attDoc.replaceItemValue("Form","fAttachment");
                    attDoc.replaceItemValue("CreateDateTime",session.createDateTime(new Date()));
                    attDoc.replaceItemValue("UpdateDateTime",session.createDateTime(new Date()));
                    attDoc.replaceItemValue("CreateUser","p101010101");
                    attDoc.replaceItemValue("ParentUNID",appDocUNID);

                    attDoc.replaceItemValue("FileName",originName);
                    attDoc.replaceItemValue("SavedName",savedName);
                    attDoc.replaceItemValue("FileSize",String.valueOf(file.length()));
                    attDoc.replaceItemValue("FilePath",saveFullFolderPath + File.separator +savedName);

                    //附件上传
                    body = attDoc.createRichTextItem("Body");
                    body.embedObject(EmbeddedObject.EMBED_ATTACHMENT,
                            null, saveFullFolderPath + File.separator +savedName , originName);
                }else{
                    standardTextItem = attDoc.getFirstItem("FileName");
                    standardTextItem.appendToTextList(originName);

                    standardTextItem = attDoc.getFirstItem("SavedName");
                    standardTextItem.appendToTextList(savedName);

                    standardTextItem = attDoc.getFirstItem("FileSize");
                    standardTextItem.appendToTextList(String.valueOf(file.length()));

                    standardTextItem = attDoc.getFirstItem("FilePath");
                    standardTextItem.appendToTextList(saveFullFolderPath + File.separator +savedName);

                    body = (RichTextItem) attDoc.getFirstItem("Body");
                    body.embedObject(EmbeddedObject.EMBED_ATTACHMENT,
                            null, saveFullFolderPath + File.separator +savedName , originName);

                }

            }

            if (attDoc != null) {
                attDoc.save(true,true);
               // fileDocList.add(attDoc);
            }



            FileResult preview=getPreivewSettings(attDoc,request,appDocUNID);
            msg.setInitialPreview(preview.getInitialPreview());
            msg.setInitialPreviewConfig(preview.getInitialPreviewConfig());
            msg.setFileIds(preview.getFileIds());
            msg.setMssDbPath(appMSSDatabase);

        } catch (Exception e) {
            e.printStackTrace();

        }finally {
            try {
                NotesThread.stermThread();
                if (mssDb!=null ) mssDb.recycle();
                if (session!=null) session.recycle();
                if (attDoc!=null ) attDoc.recycle();
                if (body!=null ) body.recycle();

            } catch (NotesException e) {
                e.printStackTrace();
            }
        }



        System.out.println("JVM totalMemory:"+Runtime.getRuntime().totalMemory());
        System.out.println("JVM freeMemory:"+Runtime.getRuntime().freeMemory());

        /**
         * 返回json
         */
        String msgReturn = JSON.toJSON(msg).toString();
        //out.write("{\"name\":\"舞蹈家\"}");
        out.write(msgReturn);
        out.flush();
        out.close();

    }


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




    /**
     * 上传完成后回填已有文件的缩略图
     * @param attachmentDoc 文件列表
     * @param request
     * @return initialPreiview initialPreviewConfig fileIds
     */
    public static FileResult getPreivewSettingsUpload(Document attachmentDoc,HttpServletRequest request,String appDocUNID) throws NotesException {
        FileResult fileResult=new FileResult();
        List<String> previews=new ArrayList<String>();
        List<FileResult.PreviewConfig> previewConfigs=new ArrayList<FileResult.PreviewConfig>();

        String mssDbPath = attachmentDoc.getParentDatabase().getFilePath();
        //fileName
        Item originNameItem = attachmentDoc.getFirstItem("FileName");


        String[] fileArr=new String[originNameItem.getValueLength()];
        int index=0;
        Document temp = null;

        Enumeration values = originNameItem.getValues().elements();
        while (values.hasMoreElements()) {
            String originName = (String)values.nextElement();

            previews.add("<div class='kv-preview-data file-preview-other-frame'><div class='file-preview-other'>" +
                    "<span class='file-other-icon'>"+getFileIcon(originName)+"</span></div></div>");

            //上传后预览配置
            FileResult.PreviewConfig previewConfig=new FileResult.PreviewConfig();
            previewConfig.setWidth("120px");
            previewConfig.setCaption(originName);
            previewConfig.setKey(attachmentDoc.getUniversalID());
            // previewConfig.setUrl(request.getContextPath()+"/file/delete");
            previewConfig.setExtra(new FileResult.PreviewConfig.Extra(attachmentDoc.getUniversalID(),appDocUNID,mssDbPath));
            previewConfig.setSize(1234567L);
            previewConfigs.add(previewConfig);


        }

        fileResult.setInitialPreview(previews);
        fileResult.setInitialPreviewConfig(previewConfigs);
        fileResult.setFileIds(attachmentDoc.getUniversalID());
        return fileResult;
    }





    /**
     * 回填已有文件的缩略图
     * @param attachmentDoc 文件列表
     * @param request
     * @return initialPreiview initialPreviewConfig fileIds
     */
    public static FileResult getPreivewSettings(Document attachmentDoc,HttpServletRequest request,String appDocUNID) throws NotesException {
        FileResult fileResult=new FileResult();
        List<String> previews=new ArrayList<String>();
        List<FileResult.PreviewConfig> previewConfigs=new ArrayList<FileResult.PreviewConfig>();

        String mssDbPath = attachmentDoc.getParentDatabase().getFilePath();
        //fileName
        Item originNameItem = attachmentDoc.getFirstItem("FileName");

        //web根目录绝对路径
        //String dirPath = request.getRealPath("/"); /local/notesdata03/domino/html/

        String[] fileArr=new String[originNameItem.getValueLength()];
        int index=0;
        Document temp = null;

        Enumeration values = originNameItem.getValues().elements();
        while (values.hasMoreElements()) {
            String originName = (String)values.nextElement();

            previews.add("<div class='kv-preview-data file-preview-other-frame'><div class='file-preview-other'>" +
                    "<span class='file-other-icon'>"+getFileIcon(originName)+"</span></div></div>");

            //上传后预览配置
            FileResult.PreviewConfig previewConfig=new FileResult.PreviewConfig();
            previewConfig.setWidth("120px");
            previewConfig.setCaption(originName);
            previewConfig.setKey(attachmentDoc.getUniversalID());
            // previewConfig.setUrl(request.getContextPath()+"/file/delete");
            previewConfig.setExtra(new FileResult.PreviewConfig.Extra(attachmentDoc.getUniversalID(),appDocUNID,mssDbPath));
            previewConfig.setSize(1234567L);
//            if(attFileName.indexOf(".txt")>0)
//                previewConfig.setType("text");
//            if(attFileName.indexOf(".pdf")>0)
//                previewConfig.setType("pdf");
            previewConfigs.add(previewConfig);


        }


/*
        for (Document sysFile : fileList) {
            //上传后预览 TODO 该预览样式暂时不支持theme:explorer的样式，后续可以再次扩展
            //如果其他文件可预览txt、xml、html、pdf等 可在此配置
            String mssDbPath = sysFile.getParentDatabase().getFilePath();
            String attDocUnid = sysFile.getUniversalID();
            String attSavedName = sysFile.getItemValueString("SavedName");
            String attFileName = sysFile.getItemValueString("FileName");
            String _atturl = "/"+ mssDbPath + "/0/" + attDocUnid + "/$file/" + attSavedName;
            System.out.println("_atturl:"+_atturl);
            //数据的形式展示
            //previews.add(_atturl);

            //if(FileUtil.isImage(sysFile.getItemValueString("FilePath"))) {
                //拼凑可以直接访问附件的URL  = "/" + mssdb + "/0/" + attunid + "/$file/" + encodeURIComponent(attfile);
                //previews.add("<img src='" + _atturl + "' class='file-preview-image kv-preview-data' " +
                 //       "style='width:auto;height:160px' alt='" + attFileName + " title='" + attFileName + "''>");
            //}else if(attFileName.indexOf(".pdf")>0) {
            //    previews.add(_atturl);
            //}else{
                previews.add("<div class='kv-preview-data file-preview-other-frame'><div class='file-preview-other'>" +
                      "<span class='file-other-icon'>"+getFileIcon(sysFile.getItemValueString("FileName"))+"</span></div></div>");
            //}

            //上传后预览配置
            FileResult.PreviewConfig previewConfig=new FileResult.PreviewConfig();
            previewConfig.setWidth("120px");
            previewConfig.setCaption(sysFile.getItemValueString("FileName"));
            previewConfig.setKey(sysFile.getUniversalID());
            // previewConfig.setUrl(request.getContextPath()+"/file/delete");
            previewConfig.setExtra(new FileResult.PreviewConfig.Extra(sysFile.getUniversalID(),appDocUNID,mssDbPath));
            previewConfig.setSize((long) sysFile.getItemValueInteger("FileSize"));
//            if(attFileName.indexOf(".txt")>0)
//                previewConfig.setType("text");
//            if(attFileName.indexOf(".pdf")>0)
//                previewConfig.setType("pdf");
            previewConfigs.add(previewConfig);
            fileArr[index++]=sysFile.getUniversalID();
        }
*/

        fileResult.setInitialPreview(previews);
        fileResult.setInitialPreviewConfig(previewConfigs);
        fileResult.setFileIds(attachmentDoc.getUniversalID());
        return fileResult;
    }


    /**
     * 根据文件名获取icon
     * @param fileName 文件
     * @return
     */
    public static String getFileIcon(String fileName){
        String ext= StrUtil.getExtName(fileName);
        return fileIconMap.get(ext)==null?fileIconMap.get("default").toString():fileIconMap.get(ext).toString();
    }

}
