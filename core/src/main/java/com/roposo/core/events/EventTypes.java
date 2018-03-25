package com.roposo.core.events;

/**
 * @author anilshar on 1/13/16.
 */
public class EventTypes {

    private static int totalEvents = 1;

    public static final int didBlockedUser = totalEvents++;
    public static final int didUnFollowedUser = totalEvents++;
    public static final int didFollowedUser = totalEvents++;
    public static final int replyAsCommentToUser = totalEvents++;
    public static final int doubleTapHint = totalEvents++;
    public static final int list_title_text_changed = totalEvents++;
    public static final int featureShowcaseRequested = totalEvents++;
    public static final int storyLocationUpdated = totalEvents++;
    public static final int storyUpdated = totalEvents++;
    public static final int albumsDidLoaded = totalEvents++;
    public static final int screenshotTook = totalEvents++;
    public static final int profileLocationUpdated = totalEvents++;
    public static final int productLikeChanged = totalEvents++;
    public static final int interestAdded = totalEvents++;
    public static final int storyCreationStarted = totalEvents++;
    public static final int storyProcessingFinished = totalEvents++;
    public static final int storyCreationFinished = totalEvents++;
    public static final int storyCreationCanceled = totalEvents++;
    public static final int storyCreationFailed = totalEvents++;
    public static final int storyTransferStatus = totalEvents++;
    public static final int storyTransferProgress = totalEvents++;
    public static final int storyCreated = totalEvents++;
    public static final int stickerAddedOrRemoved = totalEvents++;
    public static final int stickerMoving = totalEvents++;
    public static final int appConfigFetched = totalEvents++;
    public static final int notificationCountUpdated = totalEvents++;
    public static final int notificationCountUpdatedForChat = totalEvents++;
    public static final int isChatAllowed = totalEvents++;
    public static final int contactSelectUnselect = totalEvents++;
    //chat related event types
    public static final int chatMessageReceived = totalEvents++;
    public static final int chatInvitationAccepted = totalEvents++;
    public static final int chatUserUnblocked = totalEvents++;
    public static final int chatUserPresenceChanged = totalEvents++;
    public static final int chatLoadHistoryClicked = totalEvents++;
    public static final int startLikeGame = totalEvents++;
    public static final int likeGameHeartCollected = totalEvents++;
    public static final int imglyImageProcessed = totalEvents++;
    public static final int chatRequestReceived = totalEvents++;
    public static final int onChatAuthenticated = totalEvents++;
    public static final int incrementLikeCount = totalEvents++;
    public static final int incrementFollowCount = totalEvents++;
    public static final int photoCommentUploadFailed = totalEvents++;
    public static final int photoCommentUploadSuccess = totalEvents++;
    public static final int resetCommentEid = totalEvents++;
    public static final int offlineUpdateComplete = totalEvents++;
    public static final int chatItemClicked = totalEvents++;
    public static final int chatPushMessage = totalEvents++;
    public static final int chatAcknowledgmentReceived = totalEvents++;
    public static final int refreshStickerPager = totalEvents++;
    public static final int stickerPagerTabLoadingFailed = totalEvents++;
    public static final int refreshIfRecent = totalEvents++;
    public static final int onSongDownload = totalEvents++;
    public static final int latestDownload = totalEvents++;
    public static final int postMessageInChat = totalEvents++;
    public static final int productShortListed = totalEvents++;
    public static final int updateCart = totalEvents++;
    public static final int updateCartCounter = totalEvents++;
    public static final int addStoryInChat = totalEvents++;
    public static final int chatImageUploadSuccess = totalEvents++;
    public static final int chatMessageSent = totalEvents++;
    public static final int chatMessageSeen = totalEvents++;
    public static final int festiveCardReady = totalEvents++;
    public static final int deleteChat = totalEvents++;
    public static final int otpListener = totalEvents++;
    public static final int uiCardsReceived = totalEvents++;
    public static final int uiCardsDismissed = totalEvents++;
    public static final int chatItemSent = totalEvents++;
    public static final int chatItemDelivered = totalEvents++;
    public static final int chatItemDisplayed = totalEvents++;
    public static final int localChatUsersModified = totalEvents++;
    public static final int chatUserListUpdated = totalEvents++;

    public static final int channelClosed = totalEvents++;
    public static final int startCounter = totalEvents++;
    public static final int stopCounter = totalEvents++;
    public static final int panelStateChanged = totalEvents++;
    public static final int emptyChannel = totalEvents++;
    public static final int refreshExploreTab = totalEvents++;
    public static final int requestAcceptedRejected = totalEvents++;

    public static final int mediaEntryUpdated = totalEvents++;
    public static final int newMessageSent = totalEvents++;
    public static final int audioListDidLoaded = totalEvents++;
    public static final int onPhotoUpdate = totalEvents++;
    public static final int onChannelPause = totalEvents++;
    public static final int storyDeleted = totalEvents++;
    public static final int userViewUpdate = totalEvents++;
    public static final int loadProfile = totalEvents++;
    public static final int showChannelPreview = totalEvents++;
    public static final int showNavTip = totalEvents++;
    public static final int removeChannelSuggestion = totalEvents++;
    public static final int loadPeopleInNetowrk = totalEvents++;
    public static final int refreshReactionSheet = totalEvents++;
    public static final int onPageChanged = totalEvents++;
    public static final int onLowBattery = totalEvents++;
    public static final int showIntroVideo = totalEvents++;
    public static final int refreshDiscover = totalEvents++;
    public static final int postComment = totalEvents++;
    public static final int showLoader = totalEvents++;
    public static final int entityLoaded = totalEvents++;
    public static final int onMiniChannelClick = totalEvents++;
    public static final int creationConfigUpdated = totalEvents++;
    public static final int profileContestViewUpdate = totalEvents++;
    public static final int hideLoader = totalEvents++;
    public static final int refreshInviteUrl = totalEvents++;
    public static final int animateChannel = totalEvents++;
    public static final int tryChannelCall = totalEvents++;
    public static final int channelSwitch = totalEvents++;
    public static final int particleEffectSelected = totalEvents++;
    public static final int RUN_WALLET_ANIM = totalEvents++;
    public static final int onFilterEffectSelected = totalEvents++;
    public static final int onFilterEffectDeselected = totalEvents++;
    public static final int storyPreparingProgress = totalEvents++;

}
