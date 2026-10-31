package org.dandeliondaily.shared.render;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class QuickCaptureRendererTest {

    private String render(QuickCaptureRenderer.Config config) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        new QuickCaptureRenderer().render(out, config);
        out.flush();
        return sw.toString();
    }

    @Test
    public void render_offersScheduleThenEditAction() {
        String html = render(new QuickCaptureRenderer.Config("FocusedActionServlet"));

        int schedule = html.indexOf("value=\"Schedule\"");
        int edit = html.indexOf("value=\"Schedule and Edit\"");
        Assert.assertTrue(schedule > 0);
        Assert.assertTrue("Schedule must come first so Enter submits it", edit > schedule);
        Assert.assertFalse(html.contains("Schedule and Start"));
        Assert.assertTrue(html.contains("action=\"FocusedActionServlet\""));
    }

    @Test
    public void render_postsHiddenFieldsAndEscapesProjectNames() {
        String html = render(new QuickCaptureRenderer.Config("PlanAheadServlet")
                .hidden("windowStart", "2026-11-02")
                .projectNames(Arrays.asList("Work \"Hygiene\"", "</script><b>")));

        Assert.assertTrue(html.contains("name=\"windowStart\" value=\"2026-11-02\""));
        Assert.assertTrue(html.contains("[\"Work \\\"Hygiene\\\"\",\"\\u003c/script>\\u003cb>\"]"));
        Assert.assertFalse(html.contains("</script><b>"));
    }
}
