package org.openimmunizationsoftware.pt.api.common;

import java.util.Date;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.WebApiClient;

public class WebApiClientDao {

    public WebApiClient findByApiKey(String apiKey) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery("from WebApiClient where apiKey = ?");
        query.setString(0, apiKey);
        return (WebApiClient) query.uniqueResult();
    }

    public void touchLastUsedDate(WebApiClient client, Date when) {
        if (client == null) {
            return;
        }
        client.setLastUsedDate(when);
        HibernateRequestContext.getCurrentSession().update(client);
    }
}
