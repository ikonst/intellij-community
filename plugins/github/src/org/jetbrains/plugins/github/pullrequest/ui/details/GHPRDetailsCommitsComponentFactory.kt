// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.github.pullrequest.ui.details

import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil
import com.intellij.collaboration.ui.util.bindDisabled
import com.intellij.collaboration.ui.util.bindText
import com.intellij.collaboration.ui.util.bindVisibility
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.ActionLink
import com.intellij.ui.popup.PopupState
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.InlineIconButton
import com.intellij.util.ui.JBFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.plugins.github.api.data.GHCommit
import org.jetbrains.plugins.github.pullrequest.ui.details.model.GHPRCommitsViewModel
import org.jetbrains.plugins.github.pullrequest.ui.toolwindow.GHPRDiffController
import java.awt.event.ActionListener
import javax.swing.JComponent
import javax.swing.JLabel

internal object GHPRDetailsCommitsComponentFactory {
  private const val COMPONENTS_GAP = 4

  fun create(scope: CoroutineScope, commitsVm: GHPRCommitsViewModel, diffBridge: GHPRDiffController): JComponent {
    val commitsPopupTitle = JLabel().apply {
      font = JBFont.regular().asBold()
      bindText(scope, commitsVm.reviewCommits.map { commits ->
        CollaborationToolsBundle.message("review.details.commits.title.text", commits.size)
      })
    }
    val commitsPopup = createCommitChooserActionLink(scope, commitsVm, diffBridge)
    val nextCommitIcon = InlineIconButton(AllIcons.Chooser.Bottom).apply {
      withBackgroundHover = true
      actionListener = ActionListener { commitsVm.selectNextCommit() }
      bindVisibility(scope, commitsVm.selectedCommit.map { it != null })
      bindDisabled(scope, combine(commitsVm.selectedCommitIndex, commitsVm.reviewCommits) { selectedCommitIndex, commits ->
        selectedCommitIndex == commits.size - 1
      })
    }
    val previousCommitIcon = InlineIconButton(AllIcons.Chooser.Top).apply {
      withBackgroundHover = true
      actionListener = ActionListener { commitsVm.selectPreviousCommit() }
      bindVisibility(scope, commitsVm.selectedCommit.map { it != null })
      bindDisabled(scope, commitsVm.selectedCommitIndex.map { it == 0 })
    }

    return HorizontalListPanel(COMPONENTS_GAP).apply {
      add(commitsPopupTitle)
      add(commitsPopup)
      add(nextCommitIcon)
      add(previousCommitIcon)
    }
  }

  private fun createCommitChooserActionLink(
    scope: CoroutineScope,
    commitsVm: GHPRCommitsViewModel,
    diffBridge: GHPRDiffController
  ): JComponent {
    return ActionLink().apply {
      setDropDownLinkIcon()
      bindText(scope, combine(commitsVm.selectedCommit, commitsVm.reviewCommits) { selectedCommit, commits ->
        selectedCommit?.abbreviatedOid ?: CollaborationToolsBundle.message("review.details.commits.popup.text", commits.size)
      })
      bindDisabled(scope, commitsVm.reviewCommits.map { commits ->
        commits.size == 1
      })

      addActionListener { event ->
        val popupState: PopupState<JBPopup> = PopupState.forPopup()
        val parentComponent = event.source as? JComponent ?: return@addActionListener
        val point = RelativePoint.getSouthWestOf(parentComponent)
        scope.launch {
          val commits = commitsVm.reviewCommits.value
          val popupItems: List<GHCommit?> = mutableListOf<GHCommit?>(null).apply {
            addAll(commits)
          }

          val selectedCommit = ChooserPopupUtil.showChooserPopup(point, popupState, popupItems) { selectedCommit ->
            val title = selectedCommit?.messageHeadline
                        ?: CollaborationToolsBundle.message("review.details.commits.popup.all", commits.size)
            val isSelected = selectedCommit == commitsVm.selectedCommit.value
            val icon = if (isSelected) AllIcons.Actions.Checked_selected else JBUIScale.scaleIcon(EmptyIcon.create(12))

            return@showChooserPopup ChooserPopupUtil.PopupItemPresentation.Simple(title, icon)
          }

          commitsVm.selectCommit(selectedCommit)
          diffBridge.activeTree = GHPRDiffController.ActiveTree.COMMITS
        }
      }
    }
  }
}