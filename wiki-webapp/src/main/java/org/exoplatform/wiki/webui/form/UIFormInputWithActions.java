/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.wiki.webui.form;

import java.io.Serializable;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


@Serialized
public class UIFormInputWithActions extends UIFormInputSet {

  /**
   * .
   */
  private static final Log LOG = ExoLogger.getLogger(UIFormInputWithActions.class);

  Map<String, List<ActionData>> actionField = new HashMap<String, List<ActionData>>();

  public UIFormInputWithActions() {
  }

  public UIFormInputWithActions(String id) {
    super.setId(id);
  }

  public void setActionField(String fieldName, List<ActionData> actions) {
    actionField.put(fieldName, actions);
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    if (getComponentConfig() != null) {
      super.processRender(context);
      return;
    }
    UIForm uiForm = getAncestorOfType(UIForm.class);
    Writer w = context.getWriter();
    w.write("<div id='" + getId() + "' class='uiFormInputSet  " + getId() + "'>");
    w.write("<table class='uiFormGrid'>");
    ResourceBundle res = context.getApplicationResourceBundle();

    for (UIComponent inputEntry : getChildren()) {
      if (inputEntry.isRendered()) {
        String label;
        try {
          label = uiForm.getLabel(res, inputEntry.getId());
          if (inputEntry instanceof UIFormInputBase) {
        	  UIFormInputBase input = ((UIFormInputBase) inputEntry);
        	  input.setLabel(label);
        	  input.setHTMLAttribute("placeholder", label);
          }
        } catch (MissingResourceException ex) {
          label = inputEntry.getId();
          LOG.error("\n " + uiForm.getId() + ".label." + inputEntry.getId() + " not found value");
        }
        w.write("<tr>");        
        w.write("<td class='fieldInput'>");
        renderUIComponent(inputEntry);
        List<ActionData> actions = actionField.get(inputEntry.getName());
        if (actions != null) {
        w.write("<span class='groupPermissionBox'>");    	
          for (ActionData action : actions) {
            String actionLabel;
            try {
              actionLabel = uiForm.getLabel(res, "action." + action.getActionName());
            } catch (MissingResourceException ex) {
              actionLabel = action.getActionName();
              LOG.debug("Key: '" + uiForm.getId() + ".label.action." + action.getActionName() + "' not found");
            }
            String actionLink;
            if (action.getActionParameter() != null) {
              actionLink = getParent().event(action.getActionListener(), action.getActionParameter());
            } else {
              actionLink = getParent().event(action.getActionListener());
            }
            
            if (action.getActionType() == ActionData.TYPE_ICON) {
              w.write("<a class='actionIcon' title='" + actionLabel + "' onclick=\"" + actionLink + "\" data-placement='bottom' rel='tooltip' >" 
                + "<i class='" + action.getCssIconClass() + " uiIconLightGray'></i></a>");
              if (action.isShowLabel)
                w.write(actionLabel);
            } else if (action.getActionType() == ActionData.TYPE_LINK) {
              w.write("<a href=\"" + actionLink + "\">" + actionLabel + "</a>");
            } else if (action.getActionType() == ActionData.TYPE_BUTTON) {
              w.write("&nbsp;&nbsp;<button type='button' class='btn' onclick=\"" + actionLink + "\">" + actionLabel + "</button>");
            }
            w.write("&nbsp;");
            if (action.isBreakLine())
              w.write("<br/>");
          }
          w.write("</span>");
        }
        w.write("</td>");
        w.write("</tr>");
      }
    }
    w.write("</table>");
    w.write("</div>");
  }

  static public class ActionData implements Serializable {
    final public static int TYPE_ICON       = 0;

    final public static int TYPE_LINK       = 1;

    final public static int TYPE_BUTTON     = 2;

    private int             actionType      = 0;

    private String          actionName;

    private String          actionListener;

    private String          actionParameter = null;

    private String          cssIconClass    = "AddNewNodeIcon";

    private boolean         isShowLabel     = false;

    private boolean         isBreakLine     = false;

    public void setActionType(int actionType) {
      this.actionType = actionType;
    }

    public int getActionType() {
      return actionType;
    }

    public void setActionName(String actionName) {
      this.actionName = actionName;
    }

    public String getActionName() {
      return actionName;
    }

    public void setActionListener(String actionListener) {
      this.actionListener = actionListener;
    }

    public String getActionListener() {
      return actionListener;
    }

    public void setActionParameter(String actionParameter) {
      this.actionParameter = actionParameter;
    }

    public String getActionParameter() {
      return actionParameter;
    }

    public void setCssIconClass(String cssIconClass) {
      this.cssIconClass = cssIconClass;
    }

    public String getCssIconClass() {
      return cssIconClass;
    }

    public void setShowLabel(boolean isShowLabel) {
      this.isShowLabel = isShowLabel;
    }

    public boolean isShowLabel() {
      return isShowLabel;
    }

    public void setBreakLine(boolean isBreakLine) {
      this.isBreakLine = isBreakLine;
    }

    public boolean isBreakLine() {
      return isBreakLine;
    }
  }
}