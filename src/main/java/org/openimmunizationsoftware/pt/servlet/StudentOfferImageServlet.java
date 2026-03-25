package org.openimmunizationsoftware.pt.servlet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.StudentOffer;
import org.openimmunizationsoftware.pt.doa.StudentOfferTemplateDao;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.StudentOfferTemplate;
import org.openimmunizationsoftware.pt.model.WebUser;

@MultipartConfig
public class StudentOfferImageServlet extends ClientServlet {

    private static final String PARAM_MODE = "mode";
    private static final String MODE_VIEW = "view";
    private static final String PARAM_SIZE = "size";

    private static final String PARAM_STUDENT_OFFER_TEMPLATE_ID = "studentOfferTemplateId";
    private static final String PARAM_STUDENT_OFFER_ID = "studentOfferId";
    private static final String PARAM_RETURN_ANCHOR = "returnAnchor";
    private static final String PARAM_IMAGE_FILE = "imageFile";

    private static final String ACTION_UPLOAD = "Upload Image";

    private static final int IMAGE_SIZE = 512;
    private static final String DEFAULT_BASE_FOLDER = "/var/lib/dandelion/student-offer-images";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String mode = n(request.getParameter(PARAM_MODE));
        if (MODE_VIEW.equalsIgnoreCase(mode)) {
            renderImage(request, response);
            return;
        }

        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            if ("STUDENT".equalsIgnoreCase(n(webUser.getWorkflowType()))) {
                response.sendRedirect("HomeServlet");
                return;
            }

            Session dataSession = appReq.getDataSession();
            StudentOfferTemplateDao dao = new StudentOfferTemplateDao(dataSession);
            Integer offerTemplateId = parseInteger(request.getParameter(PARAM_STUDENT_OFFER_TEMPLATE_ID));
            if (offerTemplateId == null) {
                response.sendRedirect("StudentOfferSetupServlet");
                return;
            }

            StudentOfferTemplate offerTemplate = dao.getById(offerTemplateId.intValue());
            if (!canEdit(offerTemplate, webUser)) {
                response.sendRedirect("StudentOfferSetupServlet");
                return;
            }

            String returnAnchor = n(request.getParameter(PARAM_RETURN_ANCHOR));
            if (returnAnchor.equals("")) {
                returnAnchor = "offer-" + offerTemplate.getStudentOfferTemplateId();
            }

            String action = n(request.getParameter("action"));
            if (ACTION_UPLOAD.equals(action)) {
                try {
                    Part imagePart = request.getPart(PARAM_IMAGE_FILE);
                    if (imagePart == null || imagePart.getSize() == 0) {
                        appReq.setMessageProblem("Please choose an image file to upload.");
                    } else if (!isAllowedImage(imagePart)) {
                        appReq.setMessageProblem("Unsupported image file type. Please upload JPG, PNG, or WEBP.");
                    } else {
                        BufferedImage source = ImageIO.read(imagePart.getInputStream());
                        if (source == null) {
                            appReq.setMessageProblem("Unable to read the uploaded image.");
                        } else {
                            File baseFolder = resolveImageBaseFolder(dataSession);
                            if (!baseFolder.exists() && !baseFolder.mkdirs()) {
                                appReq.setMessageProblem("Image folder does not exist and could not be created: "
                                        + baseFolder.getAbsolutePath());
                            } else {
                                String fileName = UUID.randomUUID().toString() + ".jpg";
                                BufferedImage processed = centerCropAndResize(source, IMAGE_SIZE);
                                File outputFile = new File(baseFolder, fileName);
                                ImageIO.write(processed, "jpg", outputFile);

                                Transaction trans = dataSession.beginTransaction();
                                try {
                                    offerTemplate.setImagePath(fileName);
                                    offerTemplate.setUpdatedDate(new Date());
                                    dao.update(offerTemplate);
                                    trans.commit();
                                } catch (RuntimeException e) {
                                    trans.rollback();
                                    throw e;
                                }

                                response.sendRedirect(buildSetupUrl(returnAnchor));
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    appReq.setMessageProblem("Image upload failed: " + n(e.getMessage()));
                }
            }

            appReq.setTitle("Offer Image Upload");
            printHtmlHead(appReq);
            printPage(appReq, offerTemplate, returnAnchor, dataSession);
            printHtmlFoot(appReq);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void printPage(AppReq appReq, StudentOfferTemplate offerTemplate, String returnAnchor, Session dataSession)
            throws IOException {
        PrintWriter out = appReq.getOut();
        printDandelionLocation(out, "Setup / Student Reward Store / Upload Offer Image");

        String imageUrl = "StudentOfferImageServlet?mode=view&studentOfferTemplateId="
                + offerTemplate.getStudentOfferTemplateId() + "&size=full";

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Upload Offer Image</th></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Offer</th><td class=\"boxed\">"
                + h(n(offerTemplate.getTitle())) + "</td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Current Image</th><td class=\"boxed\"><img src=\""
                + imageUrl
                + "\" alt=\"Offer image\" width=\"200\" height=\"200\" style=\"object-fit:cover;border:1px solid #ccc;\"></td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Image Folder</th><td class=\"boxed\">"
                + h(resolveImageBaseFolder(dataSession).getAbsolutePath()) + "</td></tr>");
        out.println("</table>");

        out.println("<br/>");
        out.println("<form action=\"StudentOfferImageServlet\" method=\"POST\" enctype=\"multipart/form-data\">");
        out.println("<input type=\"hidden\" name=\"" + PARAM_STUDENT_OFFER_TEMPLATE_ID + "\" value=\""
                + offerTemplate.getStudentOfferTemplateId() + "\">");
        out.println("<input type=\"hidden\" name=\"" + PARAM_RETURN_ANCHOR + "\" value=\"" + h(returnAnchor)
                + "\">");
        out.println("<table class=\"boxed\">");
        out.println(
                "  <tr class=\"boxed\"><th class=\"boxed\">File</th><td class=\"boxed\"><input type=\"file\" name=\""
                        + PARAM_IMAGE_FILE + "\" accept=\"image/jpeg,image/png,image/webp\"></td></tr>");
        out.println(
                "  <tr class=\"boxed\"><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\""
                        + ACTION_UPLOAD + "\"> <a class=\"button\" href=\"" + buildSetupUrl(returnAnchor)
                        + "\">Back</a></td></tr>");
        out.println("</table>");
        out.println("</form>");
        out.println("<p class=\"small\">Images are auto-cropped to the center square and resized to 512x512.</p>");
    }

    private void renderImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                writePlaceholderImage(response, 64);
                return;
            }
            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            Integer offerTemplateId = parseInteger(request.getParameter(PARAM_STUDENT_OFFER_TEMPLATE_ID));
            Integer studentOfferId = parseInteger(request.getParameter(PARAM_STUDENT_OFFER_ID));
            String size = n(request.getParameter(PARAM_SIZE));
            int placeholderSize = "thumb".equalsIgnoreCase(size) ? 64 : 200;

            String imagePath = "";

            if (studentOfferId != null) {
                StudentOffer offer = (StudentOffer) dataSession.get(StudentOffer.class, studentOfferId.intValue());
                if (offer == null || offer.getContact() == null
                        || (offer.getContact().getContactId() != webUser.getContactId()
                                && !webUser.isUserTypeAdmin())) {
                    writePlaceholderImage(response, placeholderSize);
                    return;
                }
                imagePath = n(offer.getImagePath());
                if (imagePath.trim().equals("") && offer.getStudentOfferTemplate() != null) {
                    imagePath = n(offer.getStudentOfferTemplate().getImagePath());
                }
            } else {
                if (offerTemplateId == null) {
                    writePlaceholderImage(response, placeholderSize);
                    return;
                }

                StudentOfferTemplateDao dao = new StudentOfferTemplateDao(dataSession);
                StudentOfferTemplate offerTemplate = dao.getById(offerTemplateId.intValue());
                if (offerTemplate == null || n(offerTemplate.getImagePath()).trim().equals("")) {
                    writePlaceholderImage(response, placeholderSize);
                    return;
                }
                if (offerTemplate.getContact() == null
                        || (offerTemplate.getContact().getContactId() != webUser.getContactId()
                                && !webUser.isUserTypeAdmin())) {
                    writePlaceholderImage(response, placeholderSize);
                    return;
                }
                imagePath = n(offerTemplate.getImagePath());
            }

            if (imagePath.trim().equals("")) {
                writePlaceholderImage(response, placeholderSize);
                return;
            }

            File baseFolder = resolveImageBaseFolder(dataSession);
            File imageFile = new File(baseFolder, imagePath);
            if (!imageFile.exists() || !imageFile.isFile() || !imageFile.canRead()) {
                writePlaceholderImage(response, placeholderSize);
                return;
            }

            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                writePlaceholderImage(response, placeholderSize);
                return;
            }

            response.setContentType("image/jpeg");
            OutputStream outputStream = response.getOutputStream();
            ImageIO.write(image, "jpg", outputStream);
            outputStream.flush();
        } catch (Exception e) {
            writePlaceholderImage(response, 64);
        } finally {
            appReq.close();
        }
    }

    private boolean canEdit(StudentOfferTemplate offerTemplate, WebUser webUser) {
        return offerTemplate != null
                && offerTemplate.getContact() != null
                && offerTemplate.getContact().getContactId() == webUser.getContactId();
    }

    private boolean isAllowedImage(Part imagePart) {
        String contentType = n(imagePart.getContentType()).toLowerCase();
        String submittedName = n(imagePart.getSubmittedFileName()).toLowerCase();
        if (contentType.contains("jpeg") || contentType.contains("jpg") || contentType.contains("png")
                || contentType.contains("webp")) {
            return true;
        }
        return submittedName.endsWith(".jpg") || submittedName.endsWith(".jpeg")
                || submittedName.endsWith(".png") || submittedName.endsWith(".webp");
    }

    private BufferedImage centerCropAndResize(BufferedImage source, int outputSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        int side = Math.min(width, height);
        int x = (width - side) / 2;
        int y = (height - side) / 2;

        BufferedImage output = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, outputSize, outputSize, x, y, x + side, y + side, null);
        } finally {
            g.dispose();
        }
        return output;
    }

    private void writePlaceholderImage(HttpServletResponse response, int size) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(238, 238, 238));
            g.fillRect(0, 0, size, size);
            g.setColor(new Color(180, 180, 180));
            g.drawRect(0, 0, size - 1, size - 1);
            g.setColor(new Color(120, 120, 120));
            g.drawString("No Img", Math.max(4, size / 5), size / 2);
        } finally {
            g.dispose();
        }
        response.setContentType("image/jpeg");
        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        outputStream.flush();
    }

    private File resolveImageBaseFolder(Session dataSession) {
        String configured = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_STUDENT_OFFER_IMAGE_BASE_FOLDER,
                DEFAULT_BASE_FOLDER,
                dataSession);
        String trimmed = n(configured).trim();
        if (trimmed.equals("")) {
            trimmed = DEFAULT_BASE_FOLDER;
            TrackerKeysManager.saveApplicationKeyValue(
                    TrackerKeysManager.KEY_STUDENT_OFFER_IMAGE_BASE_FOLDER,
                    trimmed,
                    dataSession);
        } else if (!trimmed.equals(configured)) {
            TrackerKeysManager.saveApplicationKeyValue(
                    TrackerKeysManager.KEY_STUDENT_OFFER_IMAGE_BASE_FOLDER,
                    trimmed,
                    dataSession);
        }
        return new File(trimmed);
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(Integer.parseInt(n(value).trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private String buildSetupUrl(String returnAnchor) {
        if (returnAnchor == null || returnAnchor.trim().equals("")) {
            return "StudentOfferSetupServlet";
        }
        return "StudentOfferSetupServlet#" + returnAnchor;
    }

    private static String h(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
