/*
 * Copyright 2014 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaaproject.kaa.server.admin.client;

import static org.kaaproject.kaa.server.admin.shared.util.Utils.isEmpty;

import org.kaaproject.kaa.common.dto.admin.AuthResultDto;
import org.kaaproject.kaa.common.dto.admin.AuthResultDto.Result;
import org.kaaproject.kaa.server.admin.client.login.LoginView;
import org.kaaproject.kaa.server.admin.client.mvp.view.dialog.ChangePasswordDialog;
import org.kaaproject.kaa.server.admin.client.util.Utils;
import org.kaaproject.kaa.server.admin.shared.services.KaaAuthServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class Login implements EntryPoint {

   public static final String THEME = "clean"; //$NON-NLS-1$

    public static final KaaAdminResources resources = GWT.create(
            KaaAdminResources.class);

    private KaaAuthServiceAsync authService = KaaAuthServiceAsync.Util.getInstance();

    private LoginView view = GWT.create(LoginView.class);

    private Result authResult;

    @Override
    public void onModuleLoad() {
        authService.checkAuth(new AsyncCallback<AuthResultDto>() {
            @Override
            public void onFailure(Throwable caught) {
                authResult = Result.ERROR;
                showLogin();
                view.setErrorMessage(caught.toString() + ": " + caught.getMessage());
            }

            @Override
            public void onSuccess(AuthResultDto result) {
                authResult = result.getAuthResult();
                if (authResult==Result.OK) {
                    redirectToModule("kaaAdmin");
                }
                else {
                    showLogin();
                    if (authResult==Result.ERROR) {
                        view.setErrorMessage(Utils.messages.unexpectedError());
                    }
                    else if (authResult==Result.KAA_ADMIN_NOT_EXISTS) {
                        view.setInfoMessage(Utils.messages.kaaAdminNotExists());
                    }
                }
            }
        });
    }

    public void showLogin(){
        injectThemeStyleSheet();
        resources.css().ensureInjected();

        view.getLoginButton().addClickHandler(new LoginHandler());

        view.clearMessages();

        RootLayoutPanel.get().clear();
        RootLayoutPanel.get().add(view);
    }

    class LoginHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            String userName = view.getUsernameBox().getText();
            String password = view.getPasswordBox().getText();

            if (authResult==Result.KAA_ADMIN_NOT_EXISTS) {
                createKaaAdmin(userName, password);
            }
            else {
                login(userName, password);
            }
        }

    }

    private void createKaaAdmin(final String userName, final String password) {
        if (!isEmpty(userName) && !isEmpty(password)) {
            authService.createKaaAdmin(userName, password, new AsyncCallback<Void>() {

                @Override
                public void onFailure(Throwable caught) {
                    view.setErrorMessage(caught.toString() + ": " + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    login(userName, password);
                }
            });
        }
        else {
            view.setErrorMessage(Utils.messages.emptyUsernameOrPassword());
        }
    }

    private void login(final String userName, String password) {
        String postData = preparePostData("j_username="+userName,"j_password="+password);

        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, GWT.getModuleBaseURL()+"j_spring_security_check?"+postData);
        try {
            builder.sendRequest(null, new RequestCallback() {
                public void onError(Request request, Throwable exception) {
                    view.setErrorMessage(exception.getLocalizedMessage());
                }

                public void onResponseReceived(Request request, Response response) {
                    String error = response.getHeader("Error");
                    String errorType = response.getHeader("ErrorType");
                    if (!isEmpty(errorType) && "TempCredentials".equals(errorType)) {
                        //change password
                        ChangePasswordDialog.Listener listener = new ChangePasswordDialog.Listener() {
                            @Override
                            public void onChangePassword() {
                                view.clearMessages();
                            }
                            @Override
                            public void onCancel() {}
                        };
                        ChangePasswordDialog.showChangePasswordDialog(listener,
                                userName,
                                Utils.messages.tempCredentials());
                    }
                    else if (!isEmpty(error)) {
                        view.setErrorMessage(error);
                    }
                    else {
                        view.clearMessages();
                        redirectToModule("kaaAdmin");
                    }
                }
            });
        } catch (RequestException e) {
            e.printStackTrace();
        }
    }

    private static String preparePostData(String... params) {
        String ret = "";
        String sep = "";
        for (String par : params) {
          ret += sep + par;
          sep = "&";
        }
        return ret;
    }

    private void redirectToModule(String module) {
        String path = Window.Location.getPath();
        if (!path.endsWith("/")) {
            if (path.endsWith(".html") || path.endsWith(".htm")) {
                int index = path.lastIndexOf('/');
                path = path.substring(0, index+1);
            }
            else {
                path += "/";
            }
        }
        String target = path + module + "/";
        Window.Location.assign(Window.Location.createUrlBuilder().setPath(target).buildString());
    }

    /**
     * Convenience method for getting the document's head element.
     *
     * @return the document's head element
     */
    private native HeadElement getHeadElement() /*-{
      return $doc.getElementsByTagName("head")[0];
    }-*/;


    private void injectThemeStyleSheet() {
        // Choose the name style sheet based on the locale.
        String styleSheet = "gwt/" + THEME + "/" + THEME; //$NON-NLS-1$ //$NON-NLS-2$
        styleSheet += LocaleInfo.getCurrentLocale().isRTL() ? "_rtl.css" : ".css"; //$NON-NLS-1$ //$NON-NLS-2$

        // Load the GWT theme style sheet
        String modulePath = GWT.getModuleBaseURL();
        LinkElement linkElem = Document.get().createLinkElement();
        linkElem.setRel("stylesheet"); //$NON-NLS-1$
        linkElem.setType("text/css"); //$NON-NLS-1$
        linkElem.setHref(modulePath + styleSheet);
        getHeadElement().appendChild(linkElem);
   }

}