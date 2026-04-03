package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectIssue;
import org.openimmunizationsoftware.pt.model.ProjectIssueStatus;
import org.openimmunizationsoftware.pt.model.ProjectIssueType;

public class ProjectIssueDao {

    private final Session session;

    public ProjectIssueDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<ProjectIssue> listOpenIssuesForProject(Project project) {
        Query query = session.createQuery(
                "from ProjectIssue where project = :project and issueStatusString = :status order by createdDate asc");
        query.setParameter("project", project);
        query.setParameter("status", ProjectIssueStatus.OPEN.name());
        return query.list();
    }

    public ProjectIssue getById(int projectIssueId) {
        return (ProjectIssue) session.get(ProjectIssue.class, projectIssueId);
    }

    public ProjectIssue createIssue(Project project, String issueText, ProjectIssueType issueType) {
        Date now = new Date();
        ProjectIssue issue = new ProjectIssue();
        issue.setProject(project);
        issue.setIssueText(issueText);
        issue.setIssueType(issueType);
        issue.setIssueStatus(ProjectIssueStatus.OPEN);
        issue.setCreatedDate(now);
        issue.setUpdatedDate(now);
        session.save(issue);
        return issue;
    }

    public void updateIssue(ProjectIssue issue, String issueText, ProjectIssueType issueType, boolean resolve) {
        Date now = new Date();
        issue.setIssueText(issueText);
        issue.setIssueType(issueType);
        issue.setUpdatedDate(now);
        if (resolve) {
            issue.setIssueStatus(ProjectIssueStatus.RESOLVED);
            issue.setResolvedDate(now);
        }
        session.update(issue);
    }
}
