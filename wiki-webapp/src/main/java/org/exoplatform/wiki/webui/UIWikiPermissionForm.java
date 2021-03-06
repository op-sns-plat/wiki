/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.webui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;
import org.exoplatform.webui.organization.account.UIGroupSelector;
import org.exoplatform.webui.organization.account.UIUserSelector;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.Permission;
import org.exoplatform.wiki.service.PermissionEntry;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.UIWikiPortlet.PopupLevel;
import org.exoplatform.wiki.webui.core.UIWikiForm;
import org.exoplatform.wiki.webui.form.UIFormInputWithActions;
import org.exoplatform.wiki.webui.form.UIFormInputWithActions.ActionData;

@ComponentConfigs({
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiPermissionForm.gtmpl",
  events = {
    @EventConfig(listeners = UIWikiPermissionForm.AddEntryActionListener.class),
    @EventConfig(listeners = UIWikiPermissionForm.DeleteEntryActionListener.class),
    @EventConfig(listeners = UIWikiPermissionForm.OpenSelectUserFormActionListener.class),
    @EventConfig(listeners = UIWikiPermissionForm.SelectUserActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIWikiPermissionForm.OpenSelectGroupFormActionListener.class),
    @EventConfig(listeners = UIWikiPermissionForm.SelectGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIWikiPermissionForm.OpenSelectMembershipFormActionListener.class),
    @EventConfig(listeners = UIWikiPermissionForm.SelectMembershipActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIWikiPermissionForm.SaveActionListener.class),
    @EventConfig(listeners = UIWikiPermissionForm.CloseActionListener.class)
  }
),
@ComponentConfig(type = UIPopupWindow.class, id = UIWikiPermissionForm.USER_PERMISSION_POPUP_SELECTOR, template = "system:/groovy/webui/core/UIPopupWindow.gtmpl", events = {
    @EventConfig(listeners = UIWikiPermissionForm.ClosePopupActionListener.class, name = "ClosePopup"),
    @EventConfig(listeners = UIWikiPermissionForm.SelectUserActionListener.class, name = "Add", phase = Phase.DECODE),
    @EventConfig(listeners = UIWikiPermissionForm.CloseUserPopupActionListener.class, name = "Close", phase = Phase.DECODE) })
})
public class UIWikiPermissionForm extends UIWikiForm implements UIPopupComponent {

  private List<PermissionEntry> permissionEntries = new ArrayList<PermissionEntry>();

  private Scope scope;

  public final static String ANY = "any";
  
  public final static String ROOT = "root";
  
  public final static String ADD_ENTRY = "AddEntry";
  
  public final static String DELETE_ENTRY = "DeleteEntry";
  
  public final static String WIKI_PERMISSION_OWNER = "uiWikiPermissionOwner";
  
  public final static String PERMISSION_OWNER = "PermissionOwner";
  
  public final static String PERMISSION_POPUP_SELECTOR = "UIWikiPermissionPopupSelector";
  
  public final static String USER_PERMISSION_POPUP_SELECTOR = "UIWikiUserPermissionPopupSelector";
  
  public final static String OPEN_SELECT_USER_FORM = "OpenSelectUserForm";
  
  public final static String OPEN_SELECT_GROUP_FORM= "OpenSelectGroupForm";
  
  public final static String OPEN_SELECT_MEMBERSHIP_FORM= "OpenSelectMembershipForm";
  
  public final static String GROUP_ICON = "uiIconGroup";
  
  public final static String USER_ICON = "uiIconUser";
  
  public final static String MEMBERSHIP_ICON = "uiIconMembership";
  
  public final static String ADD_ICON = "ActionIcon Add";
  
  public final static String SAVE = "Save";
  
  public final static String CLOSE = "Close";
  
  private PopupLevel popupLevel = PopupLevel.L1;

  public static enum Scope {
    WIKI, PAGE
  }
  
  public UIPopupWindow getUserPermissionPopupSelector() {
    return (UIPopupWindow) getChildById(createIdByScope(USER_PERMISSION_POPUP_SELECTOR));
  }
  
  public UIPopupWindow getPermissionPopupSelector() {
    return (UIPopupWindow) getChildById(createIdByScope(PERMISSION_POPUP_SELECTOR));
  }
  
  private String createIdByScope(String defaultId) {
    if (scope == null) {
      return defaultId;
    }
    return defaultId + "_" + scope.name();
  }

  public UIWikiPermissionForm() throws Exception {
    UIPermissionGrid permissionGrid = addChild(UIPermissionGrid.class, null, null);
    permissionGrid.setPermissionEntries(this.permissionEntries);
    String [] actionNames = new String[]{OPEN_SELECT_USER_FORM, OPEN_SELECT_MEMBERSHIP_FORM,
                                         OPEN_SELECT_GROUP_FORM, ADD_ENTRY};
    String [] actionIcons = new String[]{USER_ICON, MEMBERSHIP_ICON, GROUP_ICON, ADD_ICON};
    List<ActionData> actions = new ArrayList<ActionData>();
    ActionData action;
    for (int i = 0; i < actionNames.length; ++i) {
      action = new ActionData();
      action.setActionListener(actionNames[i]);
      if (i < actionNames.length - 1) {
        action.setActionType(ActionData.TYPE_ICON);
      } else {
        action.setActionType(ActionData.TYPE_BUTTON);
      }
      action.setActionName(actionNames[i]);
      action.setCssIconClass(actionIcons[i]);
      actions.add(action);
    }
    UIFormInputWithActions owner = new UIFormInputWithActions(WIKI_PERMISSION_OWNER);
    owner.addUIFormInput(new UIFormStringInput(PERMISSION_OWNER, PERMISSION_OWNER, null));
    owner.setActionField(PERMISSION_OWNER, actions);

    addChild(owner);
    addPopupWindow();

    setActions(new String[] { SAVE, CLOSE });
  }
  
  public Scope getScope() {
    return scope;
  }
  
  public PopupLevel getPopupLevel() {
    return popupLevel;
  }

  public void setPopupLevel(PopupLevel popupLevel) {
    this.popupLevel = popupLevel;
  }
  
  public void cancelPopupAction() throws Exception {
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    UIPopupContainer popupContainer = wikiPortlet.getPopupContainer(getPopupLevel());
    popupContainer.cancelPopupAction();
    setPopupLevel(PopupLevel.L1);
  }

  private void addPopupWindow() throws Exception {
    addChild(UIPopupWindow.class, USER_PERMISSION_POPUP_SELECTOR, createIdByScope(USER_PERMISSION_POPUP_SELECTOR));
    addChild(UIPopupWindow.class, null, createIdByScope(PERMISSION_POPUP_SELECTOR));
  }
  
  private void removeAllPopupWindow() {
    List<UIComponent> children = new ArrayList<UIComponent>(getChildren());
    for (UIComponent uichild : children) {
      if(uichild instanceof UIPopupWindow) {
        removeChild(uichild.getClass());
      }
    }
  }
  
  private void closeAllPopupAction() {
    List<UIComponent> children = new ArrayList<UIComponent>(getChildren());
    for (UIComponent uichild : children) {
      if (uichild instanceof UIPopupWindow) {
        closePopupAction((UIPopupWindow) uichild);
      }
    }
  }

  private static void closePopupAction(UIPopupWindow uiPopupWindow) {
    uiPopupWindow.setUIComponent(null);
    uiPopupWindow.setShow(false);
    WebuiRequestContext rcontext = WebuiRequestContext.getCurrentInstance();
    rcontext.addUIComponentToUpdateByAjax(uiPopupWindow);
  }

  private static void openPopupAction(UIPopupWindow uiPopup, UIComponent component) {
    uiPopup.setUIComponent(component);
    uiPopup.setShow(true);
    uiPopup.setWindowSize(550, 0);
    uiPopup.setResizable(true);
    WebuiRequestContext rcontext = WebuiRequestContext.getCurrentInstance();
    rcontext.addUIComponentToUpdateByAjax(uiPopup);
  }

  public void setScope(Scope scope) throws Exception {
    this.scope = scope;
    removeAllPopupWindow();
    addPopupWindow();
    
    if (Scope.WIKI.equals(this.scope)) {
      this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW, WikiMode.EDITPAGE,
          WikiMode.ADDPAGE, WikiMode.VIEWREVISION, WikiMode.SHOWHISTORY,
          WikiMode.ADVANCEDSEARCH, WikiMode.SPACESETTING});
      setActions(new String[] { SAVE });
    } else if (Scope.PAGE.equals(this.scope)) {
      this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW });
      setActions(new String[] { SAVE, CLOSE });
    }
  }

  public void setPermission(List<PermissionEntry> permissionEntries) throws Exception {
    this.permissionEntries = permissionEntries;
    UIPermissionGrid permissionGrid = getChild(UIPermissionGrid.class);
    permissionGrid.setPermissionEntries(this.permissionEntries);
  }
  
  public List<PermissionEntry> convertToPermissionEntryList(HashMap<String, String[]> permissions) {
    List<PermissionEntry> permissionEntries = new ArrayList<PermissionEntry>();
    Set<Entry<String, String[]>> entries = permissions.entrySet();
    for (Entry<String, String[]> entry : entries) {
      PermissionEntry permissionEntry = new PermissionEntry();
      String key = entry.getKey();
      IDType idType = IDType.USER;
      if (key.indexOf(":") > 0) {
        idType = IDType.MEMBERSHIP;
      } else if (key.indexOf("/") == 0) {
        idType = IDType.GROUP;
      }
      permissionEntry.setIdType(idType);
      permissionEntry.setId(key);
      Permission[] perms = new Permission[2];
      perms[0] = new Permission();
      perms[0].setPermissionType(PermissionType.VIEWPAGE);
      perms[1] = new Permission();
      perms[1].setPermissionType(PermissionType.EDITPAGE);
      for (String action : entry.getValue()) {
        if (org.exoplatform.services.jcr.access.PermissionType.READ.equals(action)) {
          perms[0].setAllowed(true);
        } else if (org.exoplatform.services.jcr.access.PermissionType.ADD_NODE.equals(action)
            || org.exoplatform.services.jcr.access.PermissionType.REMOVE.equals(action)
            || org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY.equals(action)) {
          perms[1].setAllowed(true);
        }
      }
      permissionEntry.setPermissions(perms);

      permissionEntries.add(permissionEntry);
    }
    return permissionEntries;
  }

  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }

  private void processPostAction() throws Exception {
    UIPermissionGrid permissionGrid = getChild(UIPermissionGrid.class);
    List<UIWikiPermissionEntry> uiPermissionEntries = new ArrayList<UIWikiPermissionEntry>();
    permissionGrid.findComponentOfType(uiPermissionEntries, UIWikiPermissionEntry.class);
    List<PermissionEntry> permEntries = new ArrayList<PermissionEntry>();
    for (UIWikiPermissionEntry uiPermissionEntry : uiPermissionEntries) {
      PermissionEntry permissionEntry = uiPermissionEntry.getPermissionEntry();
      if (!uiPermissionEntry.isImmutable()) {
        Permission[] permissions = permissionEntry.getPermissions();
        for (int i = 0; i < permissions.length; i++) {
          UICheckBoxInput checkboxInput = ((UIWikiPermissionEntry) uiPermissionEntry).getChildById(permissions[i].getPermissionType().name() + permissionEntry.getId());
          permissions[i].setAllowed(checkboxInput.isChecked());
        }
      }
      permEntries.add(permissionEntry);
    }
    setPermission(permEntries);
  }

  private HashMap<String, String[]> convertToPermissionMap(List<PermissionEntry> permissionEntries) {
    HashMap<String, String[]> permissionMap = new HashMap<String, String[]>();
    for (PermissionEntry permissionEntry : permissionEntries) {
      Permission[] permissions = permissionEntry.getPermissions();
      List<String> permlist = new ArrayList<String>();
      for (int i = 0; i < permissions.length; i++) {
        Permission permission = permissions[i];
        if (permission.isAllowed()) {
          if (permission.getPermissionType().equals(PermissionType.VIEWPAGE)) {
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.READ);
          } else if (permission.getPermissionType().equals(PermissionType.EDITPAGE)) {
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.ADD_NODE);
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.REMOVE);
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY);
          }
        }
      }
      if (permlist.size() > 0) {
        permissionMap.put(permissionEntry.getId(), permlist.toArray(new String[permlist.size()]));
      }
    }
    return permissionMap;
  }

  static public class AddEntryActionListener extends EventListener<UIWikiPermissionForm> {
    @Override
    public void execute(Event<UIWikiPermissionForm> event) throws Exception {
      UIWikiPermissionForm uiWikiPermissionForm = event.getSource();
      uiWikiPermissionForm.processPostAction();
      Scope scope = uiWikiPermissionForm.getScope();
      UIFormInputWithActions inputWithActions = uiWikiPermissionForm.getChild(UIFormInputWithActions.class);
      UIFormStringInput uiFormStringInput = inputWithActions.getChild(UIFormStringInput.class);
      String permissionOwner = uiFormStringInput.getValue();
      if (permissionOwner != null && permissionOwner.length() > 0) {
        OrganizationService service = uiWikiPermissionForm.getApplicationComponent(OrganizationService.class);
        StringBuilder notExistIds = new StringBuilder();
        StringBuilder duplicateIds = new StringBuilder();
        IDType idType;
        String[] entries = permissionOwner.split(",");
        for (String entry : entries) {
          if (entry.startsWith("/")) {
            idType = IDType.GROUP;
          } else if (entry.contains(":")) {
            idType = IDType.MEMBERSHIP;
          } else {
            idType = IDType.USER;
          }
          if (isExistId(entry, idType, service)) {
            if (isNotExistEntry(entry, uiWikiPermissionForm.permissionEntries)) {
              PermissionEntry permissionEntry = new PermissionEntry();
              Permission[] permissions = null;
              if (Scope.WIKI.equals(scope)) {
                permissions = new Permission[4];
                permissions[0] = new Permission();
                permissions[0].setPermissionType(PermissionType.VIEWPAGE);
                permissions[0].setAllowed(true);
                permissions[1] = new Permission();
                permissions[1].setPermissionType(PermissionType.EDITPAGE);
                permissions[2] = new Permission();
                permissions[2].setPermissionType(PermissionType.ADMINPAGE);
                permissions[3] = new Permission();
                permissions[3].setPermissionType(PermissionType.ADMINSPACE);
              } else if (Scope.PAGE.equals(scope)) {
                permissions = new Permission[2];
                permissions[0] = new Permission();
                permissions[0].setPermissionType(PermissionType.VIEWPAGE);
                permissions[0].setAllowed(true);
                permissions[1] = new Permission();
                permissions[1].setPermissionType(PermissionType.EDITPAGE);
              }
              permissionEntry.setPermissions(permissions);
              permissionEntry.setId(entry);
              permissionEntry.setIdType(idType);
              uiWikiPermissionForm.permissionEntries.add(permissionEntry);
            } else {
              if (duplicateIds.length() == 0) {
                duplicateIds.append(entry);
              } else {
                duplicateIds.append(", ").append(entry);
              }
            }
          } else {
            if (notExistIds.length() == 0) {
              notExistIds.append(entry);
            } else {
              notExistIds.append(", ").append(entry);
            }
          }
        }
        
        if (notExistIds.length() > 0) {
          String[] msgArg = { notExistIds.toString() };
          event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIWikiPermissionForm.msg.NonExistID", msgArg, ApplicationMessage.WARNING));          
        }
        
        if (duplicateIds.length() > 0) {
          String[] msgArg = { duplicateIds.toString() };
          event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIWikiPermissionForm.msg.duplicate-id", msgArg, ApplicationMessage.WARNING));          
        }
      }
      uiWikiPermissionForm.setPermission(uiWikiPermissionForm.permissionEntries);
      
      WebuiRequestContext rcontext = event.getRequestContext();
      rcontext.addUIComponentToUpdateByAjax(uiWikiPermissionForm.getChild(UIPermissionGrid.class));
      rcontext.addUIComponentToUpdateByAjax(inputWithActions);
    }

    private boolean isExistId(String identityId, IDType idType, OrganizationService service) throws Exception {
      if (idType == IDType.USER) {
        if (ANY.equalsIgnoreCase(identityId) || ROOT.equalsIgnoreCase(identityId)) {
          return true;
        }
        return service.getUserHandler().findUserByName(identityId) != null;
      } 
      
      if (idType == IDType.GROUP) {
        return service.getGroupHandler().findGroupById(identityId) != null;
      }
      
      
      String[] membership = identityId.split(":");
      Group group = service.getGroupHandler().findGroupById(membership[1]);
      if (group == null) {
        return false;
      }
      if ("*".equals(membership[0])) {
        return true;
      }
      return service.getMembershipTypeHandler().findMembershipType(membership[0]) != null;
    }

    private boolean isNotExistEntry(String entry, List<PermissionEntry> entries) {
      for (PermissionEntry permEntry : entries) {
        if (entry.equals(permEntry.getId())) {
          return false;
        }
      }
      return true;
    }
  }

  static public class DeleteEntryActionListener extends EventListener<UIWikiPermissionForm> {
    @Override
    public void execute(Event<UIWikiPermissionForm> event) throws Exception {
      UIWikiPermissionForm uiWikiPermissionForm = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIWikiPermissionEntry uiPermissionEntry = uiWikiPermissionForm.findComponentById(objectId);
      if (!uiPermissionEntry.isImmutable()) {
        uiWikiPermissionForm.permissionEntries.remove(uiPermissionEntry.getPermissionEntry());
      }
      uiWikiPermissionForm.setPermission(uiWikiPermissionForm.permissionEntries);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPermissionForm.getChild(UIPermissionGrid.class));
    }
  }

  static public class OpenSelectUserFormActionListener extends EventListener<UIWikiPermissionForm> {
    public void execute(Event<UIWikiPermissionForm> event) throws Exception {
      UIWikiPermissionForm uiWikiPermissionForm = event.getSource();
      uiWikiPermissionForm.closeAllPopupAction();
      UIPopupWindow uiPopup = uiWikiPermissionForm.getUserPermissionPopupSelector();
      UIWikiPortlet portlet = uiWikiPermissionForm.getAncestorOfType(UIWikiPortlet.class);
      UIUserSelector uiUserSelector = portlet.findFirstComponentOfType(UIUserSelector.class);
      if(uiUserSelector != null) {
        ((UIPopupWindow)uiUserSelector.getParent()).setUIComponent(null);
      }
      uiUserSelector = uiWikiPermissionForm.createUIComponent(UIUserSelector.class, null, null);
      uiUserSelector.setShowSearch(true);
      uiUserSelector.setShowSearchUser(true);
      uiUserSelector.setShowSearchGroup(false);
      openPopupAction(uiPopup, uiUserSelector);
    }
  }

  static public class SelectUserActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiUserSelector = event.getSource();
      UIWikiPermissionForm uiWikiPermissionForm = uiUserSelector.getAncestorOfType(UIWikiPermissionForm.class);
      String values = uiUserSelector.getSelectedUsers();
      UIFormInputWithActions inputWithActions = uiWikiPermissionForm.getChild(UIFormInputWithActions.class);
      UIFormStringInput uiFormStringInput = inputWithActions.getChild(UIFormStringInput.class);
      uiFormStringInput.setValue(values);      
      UIPopupWindow uiPopup = uiWikiPermissionForm.getUserPermissionPopupSelector(); 
      closePopupAction(uiPopup);
      
      WebuiRequestContext rcontext = event.getRequestContext();
      rcontext.addUIComponentToUpdateByAjax(uiWikiPermissionForm.getChildById(WIKI_PERMISSION_OWNER));
    }
  }

  static public class OpenSelectGroupFormActionListener extends EventListener<UIWikiPermissionForm> {
    @Override
    public void execute(Event<UIWikiPermissionForm> event) throws Exception {
      UIWikiPermissionForm uiWikiPermissionForm = event.getSource();
      uiWikiPermissionForm.closeAllPopupAction();
      UIGroupSelector uiGroupSelector = uiWikiPermissionForm.createUIComponent(UIGroupSelector.class, null, null);
      UIPopupWindow uiPopup = uiWikiPermissionForm.getPermissionPopupSelector();
      openPopupAction(uiPopup, uiGroupSelector);
    }
  }

  static public class SelectGroupActionListener extends EventListener<UIGroupSelector> {
    @Override
    public void execute(Event<UIGroupSelector> event) throws Exception {
      UIWikiPermissionForm uiWikiPermissionForm = event.getSource().getParent().getParent();
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIFormInputWithActions inputWithActions = uiWikiPermissionForm.getChild(UIFormInputWithActions.class);
      UIFormStringInput uiFormStringInput = inputWithActions.getChild(UIFormStringInput.class);
      uiFormStringInput.setValue("*:" + groupId);      
      closePopupAction(uiWikiPermissionForm.getPermissionPopupSelector());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPermissionForm);
    }
  }

  static public class OpenSelectMembershipFormActionListener extends EventListener<UIWikiPermissionForm> {
    public void execute(Event<UIWikiPermissionForm> event) throws Exception {
      UIWikiPermissionForm uiWikiPermissionForm = event.getSource();
      uiWikiPermissionForm.closeAllPopupAction();
      UIGroupMembershipSelector uiGroupMembershipSelector = uiWikiPermissionForm.createUIComponent(UIGroupMembershipSelector.class, null, null);
      UIPopupWindow uiPopup = uiWikiPermissionForm.getPermissionPopupSelector();
      openPopupAction(uiPopup, uiGroupMembershipSelector);
    }
  }

  static public class SelectMembershipActionListener extends EventListener<UIGroupMembershipSelector> {
    public void execute(Event<UIGroupMembershipSelector> event) throws Exception {
      UIGroupMembershipSelector uiGroupMembershipSelector = event.getSource();
      UIWikiPermissionForm uiWikiPermissionForm = uiGroupMembershipSelector.getParent().getParent();
      String currentGroup = uiGroupMembershipSelector.getCurrentGroup().getId();
      String membershipId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIFormInputWithActions inputWithActions = uiWikiPermissionForm.getChild(UIFormInputWithActions.class);
      UIFormStringInput uiFormStringInput = inputWithActions.getChild(UIFormStringInput.class);
      uiFormStringInput.setValue(membershipId + ":" + currentGroup);
      closePopupAction(uiWikiPermissionForm.getPermissionPopupSelector());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPermissionForm.getParent());
    }
  }

  static public class ClosePopupActionListener extends UIPopupWindow.CloseActionListener {
    public void execute(Event<UIPopupWindow> event) throws Exception {
       super.execute(event);
       closePopupAction(event.getSource());      
    }
  }

  static public class CloseUserPopupActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIPopupWindow uiPopup = (UIPopupWindow)event.getSource().getParent(); 
      closePopupAction(uiPopup);
    }
  }

  static public class SaveActionListener extends EventListener<UIWikiPermissionForm> {
    @Override
    public void execute(Event<UIWikiPermissionForm> event) throws Exception {
      UIWikiPermissionForm uiWikiPermissionForm = event.getSource();
      Scope scope = uiWikiPermissionForm.getScope();
      uiWikiPermissionForm.processPostAction();
      if (Scope.WIKI.equals(scope)) {
        WikiService wikiService = uiWikiPermissionForm.getApplicationComponent(WikiService.class);
        WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
        wikiService.setWikiPermission(pageParams.getType(), pageParams.getOwner(), uiWikiPermissionForm.permissionEntries);
        
        uiWikiPermissionForm.setPermission(wikiService.getWikiPermission(pageParams.getType(), pageParams.getOwner()));
        event.getRequestContext()
             .getUIApplication()
             .addMessage(new ApplicationMessage("UIWikiPermissionForm.msg.Save-permission-setting-success",
                                                null,
                                                ApplicationMessage.INFO));
        
      } else if (Scope.PAGE.equals(scope)) {
        PageImpl page = (PageImpl) Utils.getCurrentWikiPage();
        HashMap<String, String[]> permissionMap = uiWikiPermissionForm.convertToPermissionMap(uiWikiPermissionForm.permissionEntries);
        page.setPermission(permissionMap);
        page.setOverridePermission(true);
        
        // Update page info area
        UIWikiPortlet uiWikiPortlet = uiWikiPermissionForm.getAncestorOfType(UIWikiPortlet.class);
        if (page.hasPermission(PermissionType.VIEWPAGE)) {
          UIWikiPageInfoArea uiWikiPageInfoArea = uiWikiPortlet.findFirstComponentOfType(UIWikiPageInfoArea.class);
          UIWikiPageControlArea uiWikiPageControlArea = uiWikiPortlet.findFirstComponentOfType(UIWikiPageControlArea.class);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPageControlArea);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPageInfoArea);
          uiWikiPermissionForm.cancelPopupAction();
        } else {
          uiWikiPortlet.changeMode(WikiMode.PAGE_NOT_FOUND);
          event.getRequestContext().getJavascriptManager().addCustomizedOnLoadScript("eXo.wiki.UIWikiPageNotFound.hidePopup();");
          Utils.ajaxRedirect(event, Utils.getCurrentWikiPageParams(), WikiMode.PAGE_NOT_FOUND, null);
        }
      }
    }
  }

  static public class CloseActionListener extends EventListener<UIWikiPermissionForm> {
    @Override
    public void execute(Event<UIWikiPermissionForm> event) throws Exception {
      UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      UIWikiPermissionForm uiWikiPermissionForm = wikiPortlet.findFirstComponentOfType(UIWikiPermissionForm.class);
      uiWikiPermissionForm.cancelPopupAction();
    }
  }
}
