// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.plugins.newui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RetrievableIcon;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Alexander Lobas
 */
class PluginLogoIcon implements PluginLogoIconProvider {
  static final Map<Icon, Icon> disabledIcons = new WeakHashMap<>(200);

  private final Icon myPluginLogo;
  private final Icon myPluginLogoError;

  private final Icon myPluginLogoDisabled;
  private final Icon myPluginLogoDisabledError;

  private final Icon myPluginLogoBig;
  private final Icon myPluginLogoErrorBig;

  private final Icon myPluginLogoDisabledBig;
  private final Icon myPluginLogoDisabledErrorBig;

  PluginLogoIcon(@NotNull Icon logo, @NotNull Icon logoDisabled, @NotNull Icon logoBig, @NotNull Icon logoDisabledBig) {
    myPluginLogo = logo;
    myPluginLogoError = setSouthWest(logo, AllIcons.Plugins.ModifierInvalid);

    myPluginLogoDisabled = logoDisabled;
    myPluginLogoDisabledError = setSouthWest(logoDisabled, AllIcons.Plugins.ModifierInvalid);

    Icon errorLogo2x = getErrorLogo2x();

    myPluginLogoBig = logoBig;
    myPluginLogoErrorBig = setSouthWest(logoBig, errorLogo2x);

    myPluginLogoDisabledBig = logoDisabledBig;
    myPluginLogoDisabledErrorBig = setSouthWest(logoDisabledBig, errorLogo2x);
  }

  protected @NotNull Icon getDisabledIcon(@NotNull Icon icon, boolean base) {
    return createDisabledIcon(icon, base);
  }

  protected static @NotNull Icon createDisabledIcon(@NotNull Icon icon, boolean base) {
    return calculateDisabledIcon(icon, base);
  }

  private static @NotNull Icon calculateDisabledIcon(@NotNull Icon icon, boolean base) {
    Icon i = icon instanceof RetrievableIcon ? ((RetrievableIcon)icon).retrieveIcon() : icon;
    synchronized (disabledIcons) {
      return disabledIcons.computeIfAbsent(i, __->
        base
             ? IconLoader.INSTANCE.filterIcon(i, () -> new UIUtil.GrayFilter())
             : IconLoader.INSTANCE.filterIcon(i, () -> new UIUtil.GrayFilter(JBColor.isBright() ? 20 : 19, 0, 100)));
    }
  }

  protected @NotNull Icon getScaled2xIcon(@NotNull Icon icon) {
    return IconUtil.scale(icon, null, 2.0f);
  }

  private static @NotNull Icon setSouthWest(@NotNull Icon main, @NotNull Icon southWest) {
    LayeredIcon layeredIcon = new LayeredIcon(2);

    layeredIcon.setIcon(main, 0);
    layeredIcon.setIcon(southWest, 1, SwingConstants.SOUTH_WEST);

    return layeredIcon;
  }

  protected @NotNull Icon getErrorLogo2x() {
    return PluginLogo.reloadIcon(AllIcons.Plugins.ModifierInvalid, 20, 20, PluginLogo.LOG);
  }

  @Override
  public @NotNull Icon getIcon(boolean big, boolean error, boolean disabled) {
    if (error) {
      if (big) {
        return disabled ? myPluginLogoDisabledErrorBig : myPluginLogoErrorBig;
      }
      return disabled ? myPluginLogoDisabledError : myPluginLogoError;
    }
    if (big) {
      return disabled ? myPluginLogoDisabledBig : myPluginLogoBig;
    }
    return disabled ? myPluginLogoDisabled : myPluginLogo;
  }
}