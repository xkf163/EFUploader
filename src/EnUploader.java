import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lotus.domino.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 19:04 2018/8/18
 */
public class EnUploader extends HttpServlet  {


    //previewFileIconSettings
    public static Map fileIconMap = new HashMap();


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

        System.out.println("EnUploader doPost running ...");
        System.out.println("JVM totalMemory:"+Runtime.getRuntime().totalMemory());
        System.out.println("JVM freeMemory:"+Runtime.getRuntime().freeMemory());

        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        FileResult msg = new FileResult();

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
        Document attDoc =null;
        View mssView = null;
        RichTextItem body = null;

        /* 0） 删除*/
        if (action!=null & action.equals("del")){
            appDocUNID = request.getParameter("appDocUNID");
            String attDocUnid = request.getParameter("id");
            appMSSDatabase = request.getParameter("appMSSDatabase");
            String md5Code = request.getParameter("key");



            InputStream is= null;
            is = request.getInputStream();
            String bodyInfo = IOUtils.toString(is, "utf-8");
            System.out.println("入参信息："+bodyInfo);
            //参信息：key=29ceb2e501f28a35ab0df04c795a3a04&appMSSDatabase=webea%2FbbsLib1.nsf&id=282D2F58ED82023848258317003155BF&appDocUNID=08C1423E79606B7448258317002DB44E

            md5Code = bodyInfo.substring(bodyInfo.indexOf("key=")+4,bodyInfo.indexOf("&appMSSDatabase="));
            appMSSDatabase = bodyInfo.substring(bodyInfo.indexOf("&appMSSDatabase=")+16,bodyInfo.indexOf("&id="));
            attDocUnid = bodyInfo.substring(bodyInfo.indexOf("&id=")+4,bodyInfo.indexOf("&appDocUNID="));
            appDocUNID = bodyInfo.substring(bodyInfo.indexOf("&appDocUNID=")+12);

            appMSSDatabase = URLDecoder.decode(appMSSDatabase, "UTF-8");
            System.out.println(appDocUNID);
            System.out.println(attDocUnid);
            System.out.println(appMSSDatabase);
            System.out.println(md5Code);


            NotesThread.sinitThread();
            try {
                session = NotesFactory.createSession();
                mssDb = session.getDatabase(session.getServerName(),appMSSDatabase);
                mssView = mssDb.getView("AttachmentView");
                attDoc = mssView.getDocumentByKey(appDocUNID);

                if (attDoc != null) {
                    System.out.println("找到了 attdoc 了");

                    //0.1 删除body数据
                    body = (RichTextItem) attDoc.getFirstItem("Body");
                    Vector v = body.getEmbeddedObjects();
                    int vSize = v.size();
                    Enumeration e = v.elements();



                    while (e.hasMoreElements()) {
                        EmbeddedObject eo = (EmbeddedObject) e.nextElement();
                        if (eo.getType() == EmbeddedObject.EMBED_ATTACHMENT && eo.getName().indexOf(md5Code) != -1) {
                            eo.remove();
                            vSize-=1;
                        }
                    }

                    System.out.println(vSize);
                    if(vSize == 0){
                        attDoc.remove(true);
                       // body.recycle(v);
                    }else {
                        //删除其他域里的值
                        //0.2 删除text域里的数据 FileName（被包含在eo.getname里）  SavedName（包含md5code） FileSize（md5code开头） FilePath（包含md5code）
                        Item delItem = attDoc.getFirstItem("FileSize");
                        Vector vvalues = delItem.getValues();
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
                        attDoc.save(true, true);

                        //delItem.recycle(vvalues);
                    }
                }


            } catch (NotesException e) {
                e.printStackTrace();
            }finally {
                if (is != null) {
                    is.close();
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


            out.write("{\"code\":\"del is finish\"}");
            out.flush();
            out.close();

            return;
        }





        /*1）下载*/
        if (action!=null & action.equals("download")){

            InputStream is = null;
            OutputStream os = null;
            File file = null;


            String appDocUnid = request.getParameter("unid");
            String md5Code = request.getParameter("key"); //md5值，文件保存时前缀用md5值，用来匹配下载的文件
            appMSSDatabase = request.getParameter("mssdb");
            System.out.println("将要下载的文件MD5 : "+md5Code);

            // 下载网络文件
            int bytesum = 0;
            int byteread = 0;


            try {
                NotesThread.sinitThread();
                session = NotesFactory.createSession();
                mssDb = session.getDatabase(session.getServerName(), appMSSDatabase);

                mssView = mssDb.getView("AttachmentView");
                attDoc = mssView.getDocumentByKey(appDocUnid);



                if (attDoc != null){

                    body = (RichTextItem) attDoc.getFirstItem("Body");
                    Vector v = body.getEmbeddedObjects();
                    Enumeration e = v.elements();
                    while (e.hasMoreElements()) {

                        EmbeddedObject eo = (EmbeddedObject) e.nextElement();
                        if (eo.getType() == EmbeddedObject.EMBED_ATTACHMENT  && eo.getName().indexOf(md5Code) != -1) {
                            String savedName = eo.getName();
                            String fileName = savedName.substring(savedName.indexOf("_")+1);
                            //String attachmentForDownload = saveFullFolderPath + File.separator + fileName;
                            //System.out.println(attachmentForDownload);
                            //eo.extractFile(attachmentForDownload);
                            //eo.remove();

                            //if(attachmentForDownload != null & !attachmentForDownload.equals(""))
                            //    file = new File(attachmentForDownload);
                            //if (file != null && file.exists() && file.isFile()) {
                                //fileName = file.getName();
                                //fileName = eo.getName();


                                //fileName = URLEncoder.encode(fileName, "utf-8");
                                //System.out.println("将要下载的文件名2"+fileName);
                                //有中文文件名乱码的问题
                                //long filelength = file.length();
                                //is = new FileInputStream(file);
                                is = eo.getInputStream();
                                int filelength = eo.getFileSize();
                            // 设置输出的格式
                                os = response.getOutputStream();
                                //response.setContentType("application/octet-stream;charset=utf-8");
                                response.setContentType("application/octet-stream");
                                response.setContentLength( filelength);
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




                           // } else {
                           //     response.setContentType("text/html;charset=utf-8");
                           //     response.getWriter().println("<script>");
                           //     response.getWriter().println(" modals.info('文件不存在!');");
                           //     response.getWriter().println("</script>");
                            //}


                            break;
                        }
                    }

                    System.out.println("while over...");


                } else {
                    response.setContentType("text/html;charset=utf-8");
                    response.getWriter().write("<script>");
                    response.getWriter().write(" modals.info('AttachmentDoc不存在!');");
                    response.getWriter().write("</script>");
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

                file.delete();

            }

            return;
        }


        /*2）上传*/

        //request过来的附件集合
        List<FileItem> fileItemList = new ArrayList<FileItem>();

        //失败时：存放数据
        ArrayList<Integer> arr = new ArrayList<Integer>();

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
                        System.out.println("将要上传文件的大小:" + item.getSize());
                        System.out.println("将要上传文件的类型:" + item.getContentType());
                        // item.getName()返回上传文件在客户端的完整路径名称
                        System.out.println("将要上传文件的名称:" + item.getName());
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


        Item standardTextItem = null;

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
            //存放要返回到前端显示的附件s
            Map uploadMap = new HashMap();

            for(FileItem item : fileItemList){

                File tempFile = new File(item.getName());// 构造临时对象

                String md5Code = DigestUtils.md5Hex(item.getInputStream());
                System.out.println("md5:"+md5Code);

                String originName = tempFile.getName();
                int hashCode = tempFile.hashCode();
                //String filePrefix = DateUtil.format(new Date(),filePrefixFormat);
                String savedName = md5Code+ "_" + originName;
                File file = new File(saveFullFolderPath + File.separator  + savedName);
                item.write(file);// 保存文件在服务器的物理磁盘中

                if (attDoc == null) {
                    attDoc = mssDb.createDocument();
                    attDoc.replaceItemValue("Form","fAttachment");
                    attDoc.replaceItemValue("CreateDateTime",session.createDateTime(new Date()));
                    attDoc.replaceItemValue("UpdateDateTime",session.createDateTime(new Date()));
                    attDoc.replaceItemValue("CreateUser","p101010101");
                    attDoc.replaceItemValue("ParentUNID",appDocUNID);

                    //attDoc.replaceItemValue("FileName",originName);
                    //attDoc.replaceItemValue("SavedName",savedName);
                    attDoc.replaceItemValue("FileSize",md5Code+"_"+String.valueOf(file.length()));
                    //attDoc.replaceItemValue("FilePath",saveFullFolderPath + File.separator +savedName);

                    //附件上传
                    body = attDoc.createRichTextItem("Body");
                    body.embedObject(EmbeddedObject.EMBED_ATTACHMENT,
                            null, saveFullFolderPath + File.separator +savedName , originName);
                }else{
                    //standardTextItem = attDoc.getFirstItem("FileName");
                    //standardTextItem.appendToTextList(originName);

                    //standardTextItem = attDoc.getFirstItem("SavedName");
                    //standardTextItem.appendToTextList(savedName);

                    standardTextItem = attDoc.getFirstItem("FileSize");
                    standardTextItem.appendToTextList(md5Code+"_"+String.valueOf(file.length()));

                    //standardTextItem = attDoc.getFirstItem("FilePath");
                    //standardTextItem.appendToTextList(saveFullFolderPath + File.separator + savedName);

                    body = (RichTextItem) attDoc.getFirstItem("Body");
                    body.embedObject(EmbeddedObject.EMBED_ATTACHMENT,
                            null, saveFullFolderPath + File.separator +savedName , originName);

                }

                uploadMap.put(savedName , file.length());



            }

            if (attDoc != null) {
                attDoc.save(true,true);
               // fileDocList.add(attDoc);
            }

            FileResult preview=getPreivewSettingsUpload(attDoc,uploadMap,request,appDocUNID);
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
    public static FileResult getPreivewSettingsUpload(Document attachmentDoc,Map uploadMap,HttpServletRequest request,String appDocUNID) throws NotesException {
        FileResult fileResult=new FileResult();
        List<String> previews=new ArrayList<String>();
        List<FileResult.PreviewConfig> previewConfigs=new ArrayList<FileResult.PreviewConfig>();

        String mssDbPath = attachmentDoc.getParentDatabase().getFilePath();


        Iterator<Map.Entry<String, Long>> entries = uploadMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Long> entry = entries.next();
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            String hashOriginName = entry.getKey();
            String md5Code =hashOriginName.substring(0,hashOriginName.indexOf("_"));
            String originName = hashOriginName.substring(hashOriginName.indexOf("_")+1);
            Long fileSize =  entry.getValue();

            previews.add("<div class='kv-preview-data file-preview-other-frame'><div class='file-preview-other'>" +
                    "<span class='file-other-icon'>"+getFileIcon(originName)+"</span></div></div>");
            //上传后预览配置
            FileResult.PreviewConfig previewConfig=new FileResult.PreviewConfig();
            previewConfig.setWidth("120px");
            previewConfig.setCaption(originName);
            previewConfig.setKey(md5Code);
            previewConfig.setExtra(new FileResult.PreviewConfig.Extra(md5Code,appDocUNID,mssDbPath));
            previewConfig.setSize(md5Code+"_"+fileSize);
            //previewConfig.setHashCoder(md5Code);
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

        //遍历 filesize多值域，放入map（md5,filesize）
        Map fileSizeMap = new HashMap();
        Item fileSizeItem = attachmentDoc.getFirstItem("FileSize");
        Enumeration values = fileSizeItem.getValues().elements();
        while (values.hasMoreElements()) {
            String fileSizeString = (String) values.nextElement();
            String md5Code = fileSizeString.substring(0,fileSizeString.indexOf("_"));
            String fileSize = fileSizeString.substring(fileSizeString.indexOf("_")+1);
            fileSizeMap.put(md5Code,fileSize);

        }

        RichTextItem body = (RichTextItem) attachmentDoc.getFirstItem("Body");
        Vector v = body.getEmbeddedObjects();
        //表示没有附件doc没有上传附件
        if (v.size() == 0){
            return fileResult;

        }

        Enumeration e = v.elements();
        while (e.hasMoreElements()) {

            EmbeddedObject eo = (EmbeddedObject) e.nextElement();
            if (eo.getType() == EmbeddedObject.EMBED_ATTACHMENT) {
                String hashOriginName = eo.getName();

                String md5Code =hashOriginName.substring(0,hashOriginName.indexOf("_"));
                String originName = hashOriginName.substring(hashOriginName.indexOf("_")+1);

                previews.add("<div class='kv-preview-data file-preview-other-frame'><div class='file-preview-other'>" +
                        "<span class='file-other-icon'>"+getFileIcon(originName)+"</span></div></div>");

                //上传后预览配置
                FileResult.PreviewConfig previewConfig=new FileResult.PreviewConfig();
                previewConfig.setWidth("120px");
                previewConfig.setCaption(originName);
                previewConfig.setKey(md5Code);
                previewConfig.setExtra(new FileResult.PreviewConfig.Extra(attachmentDoc.getUniversalID(),appDocUNID,mssDbPath));
                previewConfig.setSize((String) fileSizeMap.get(md5Code));
                previewConfigs.add(previewConfig);

            }
        }


/*
        //SavedName
        Item originNameItem = attachmentDoc.getFirstItem("SavedName");

        //web根目录绝对路径
        //String dirPath = request.getRealPath("/"); /local/notesdata03/domino/html/

        String[] fileArr=new String[originNameItem.getValueLength()];
        int index=0;
        Document temp = null;

        values = originNameItem.getValues().elements();
        while (values.hasMoreElements()) {
            String hashOriginName = (String)values.nextElement();

            String md5Code =hashOriginName.substring(0,hashOriginName.indexOf("_"));
            String originName = hashOriginName.substring(hashOriginName.indexOf("_")+1);

            previews.add("<div class='kv-preview-data file-preview-other-frame'><div class='file-preview-other'>" +
                    "<span class='file-other-icon'>"+getFileIcon(originName)+"</span></div></div>");

            //上传后预览配置
            FileResult.PreviewConfig previewConfig=new FileResult.PreviewConfig();
            previewConfig.setWidth("120px");
            previewConfig.setCaption(originName);
            //previewConfig.setHashCoder(md5Code);
            previewConfig.setKey(md5Code);
            // previewConfig.setUrl(request.getContextPath()+"/file/delete");
            previewConfig.setExtra(new FileResult.PreviewConfig.Extra(attachmentDoc.getUniversalID(),appDocUNID,mssDbPath));
            previewConfig.setSize((String) fileSizeMap.get(md5Code));
//            if(attFileName.indexOf(".txt")>0)
//                previewConfig.setType("text");
//           if(attFileName.indexOf(".rar")>0)
//               previewConfig.setType("zip");
            previewConfigs.add(previewConfig);


        }



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
