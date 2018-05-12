/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.murati.oszk.audiobook.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.target.Target;
import com.murati.oszk.audiobook.OfflineBookService;
import com.murati.oszk.audiobook.R;
import com.murati.oszk.audiobook.utils.FavoritesHelper;
import com.murati.oszk.audiobook.utils.MediaIDHelper;


public class MediaItemViewHolder {

    public static final int STATE_INVALID = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYABLE = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;

    private static ColorStateList sColorStatePlaying;
    private static ColorStateList sColorStateNotPlaying;

    private ImageView mImageView;
    private TextView mTitleView;
    private TextView mDescriptionView;

    private ImageView mDownloadButton;
    private ImageView mFavoriteButton;


    // Returns a view for use in media item list.
    static View setupListView(final Activity activity, View convertView, ViewGroup parent,
                              MediaBrowserCompat.MediaItem item) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(activity);
        }

        final MediaItemViewHolder holder;

        Integer cachedState = STATE_INVALID;

        //Load mediaItem
        MediaDescriptionCompat description = item.getDescription();
        final String mediaId = item.getMediaId();

        // Inflate or restore view
        if (convertView == null) {

            holder = new MediaItemViewHolder();

            if (MediaIDHelper.isBrowseable(mediaId) && MediaIDHelper.isEBook(mediaId)) {
                // It is an e-book, so let's inflate with the e-book template
                convertView = LayoutInflater.
                    from(activity).
                    inflate(R.layout.fragment_ebook_item, parent, false);
            } else {
                // It is a category
                convertView = LayoutInflater.
                    from(activity).
                    inflate(R.layout.fragment_list_item, parent, false);
            }

            //Lookup the standard fields
            holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
            holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
            holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);

            convertView.setTag(holder);
        } else {
            holder = (MediaItemViewHolder) convertView.getTag();
            cachedState = (Integer) convertView.getTag(R.id.tag_mediaitem_state_cache);
        }

        //Set View Content
        holder.mTitleView.setText(description.getTitle());
        holder.mDescriptionView.setText(description.getSubtitle());

        // If the state of convertView is different, we need to adapt it
        int state = getMediaItemState(activity, item);
        if (cachedState == null || cachedState != state) {
            Drawable drawable = null;

            // Split case by browsable or by playable
            if (MediaIDHelper.isBrowseable(mediaId)) {
                // Browsable container represented by its image

                if (MediaIDHelper.isEBook(mediaId)) {
                    holder.mDownloadButton= (ImageView) convertView.findViewById(R.id.card_download);
                    holder.mDownloadButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            OfflineBookService.downloadWithActivity(mediaId, activity);
                        }
                    });

                    holder.mFavoriteButton = (ImageView) convertView.findViewById(R.id.card_favorite);
                    holder.mFavoriteButton.setImageResource(FavoritesHelper.getFavoriteIcon(mediaId));
                    holder.mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            holder.mFavoriteButton.setImageResource(
                                FavoritesHelper.toggleFavoriteWithText(mediaId, activity));
                        }
                    });
                } else {
                    //Adjust as a category
                }

                // Load URI for the item
                Uri imageUri = item.getDescription().getIconUri();
                GlideApp.
                    with(activity).
                    load(imageUri).
                    override(Target.SIZE_ORIGINAL).
                    fallback(ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.ic_navigate_books)).
                    into(holder.mImageView);

            } else {
                // Playable item represented by its state
                drawable = getDrawableByState(activity, state);
                if (drawable != null)
                    holder.mImageView.setImageDrawable(drawable);

                //holder.mImageView.setImageTintMode(PorterDuff.Mode.SRC_IN);
            }
            holder.mImageView.setVisibility(View.VISIBLE);
            convertView.setTag(R.id.tag_mediaitem_state_cache, state);
        }

        return convertView;
    }

    private static void initializeColorStateLists(Context ctx) {
        sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_not_playing));
        sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_playing));
    }

    public static Drawable getDrawableByState(Context context, int state) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(context);
        }

        switch (state) {
            case STATE_PLAYABLE:
                Drawable pauseDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_play_arrow_black_36dp);
                DrawableCompat.setTintList(pauseDrawable, sColorStateNotPlaying);
                return pauseDrawable;
            case STATE_PLAYING:
                AnimationDrawable animation = (AnimationDrawable)
                        ContextCompat.getDrawable(context, R.drawable.ic_equalizer_white_36dp);
                DrawableCompat.setTintList(animation, sColorStatePlaying);
                animation.start();
                return animation;
            case STATE_PAUSED:
                Drawable playDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_equalizer1_white_36dp);
                DrawableCompat.setTintList(playDrawable, sColorStatePlaying);
                return playDrawable;
            case STATE_NONE:
            default:
                return null;
        }
    }

    public static int getMediaItemState(Activity context, MediaBrowserCompat.MediaItem mediaItem) {
        int state = STATE_NONE;
        // Set state to playable first, then override to playing or paused state if needed
        if (mediaItem.isPlayable()) {
            state = STATE_PLAYABLE;
            if (MediaIDHelper.isMediaItemPlaying(context, mediaItem)) {
                state = getStateFromController(context);
            }
        }

        return state;
    }

    public static int getStateFromController(Activity context) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(context);
        PlaybackStateCompat pbState = controller.getPlaybackState();
        if (pbState == null ||
                pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
            return MediaItemViewHolder.STATE_NONE;
        } else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            return  MediaItemViewHolder.STATE_PLAYING;
        } else {
            return MediaItemViewHolder.STATE_PAUSED;
        }
    }
}
