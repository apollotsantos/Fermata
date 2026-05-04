package me.aap.fermata.addon.web.yt;

import static me.aap.fermata.BuildConfig.AUTO;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_BOTTOM;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_BOTTOM_LEFT;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_BOTTOM_RIGHT;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_LEFT;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_RIGHT;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_TOP;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_TOP_LEFT;
import static me.aap.fermata.media.pref.MediaPrefs.VIDEO_POSITION_TOP_RIGHT;

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.util.List;

import me.aap.fermata.FermataApplication;
import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.web.R;
import me.aap.fermata.addon.web.WebBrowserAddon;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.function.IntSupplier;
import me.aap.utils.function.Supplier;
import me.aap.utils.misc.ChangeableCondition;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.ui.fragment.ActivityFragment;

/**
 * @author Andrey Pavlenko
 */
@Keep
@SuppressWarnings("unused")
public class YoutubeAddon extends WebBrowserAddon implements PreferenceStore.Listener {
	@NonNull
	private static final AddonInfo info = FermataAddon.findAddonInfo(YoutubeAddon.class.getName());
	public static final int YT_DARK_MODE_DISABLED = 0;
	public static final int YT_DARK_MODE_ENABLED = 1;
	public static final int YT_DARK_MODE_AUTO = 2;
	private static final Pref<IntSupplier> YT_DARK_MODE = Pref.i("YT_DARK_MODE", YT_DARK_MODE_AUTO);
	private static final Pref<BooleanSupplier> YT_DESKTOP_VERSION = Pref.b("YT_DESKTOP_VERSION", false);
	private static final Pref<Supplier<String[]>> YT_BOOKMARKS = Pref.sa("YT_BOOKMARKS");
	private static final Pref<Supplier<String>> VIDEO_SCALE = Pref.s("VIDEO_SCALE", VideoScale.CONTAIN::prefName);
	private static final Pref<BooleanSupplier> YT_OPEN_ON_START = Pref.b("YT_OPEN_ON_START", false);
	private static final Pref<BooleanSupplier> YT_AUTO_HIGHEST_QUALITY =
			Pref.b("YT_AUTO_HIGHEST_QUALITY", false);
	private static final Pref<BooleanSupplier> YT_SKIP_ADD = AUTO ? Pref.b("YT_SKIP_ADD", true) : null;
	private boolean ignorePrefChange;

	@IdRes
	@Override
	public int getAddonId() {
		return me.aap.fermata.R.id.youtube_fragment;
	}

	@NonNull
	public AddonInfo getInfo() {
		return info;
	}

	@NonNull
	@Override
	public ActivityFragment createFragment() {
		return new YoutubeFragment();
	}

	@Override
	public Pref<IntSupplier> getForceDarkPref() {
		return YT_DARK_MODE;
	}

	@Override
	public Pref<BooleanSupplier> getDesktopVersionPref() {
		return YT_DESKTOP_VERSION;
	}

	@Override
	public Pref<Supplier<String[]>> getBookmarksPref() {
		return YT_BOOKMARKS;
	}

	boolean skipAd() {
		return AUTO && getPreferenceStore().getBooleanPref(YT_SKIP_ADD);
	}

	@Override
	public void contributeSettings(Context ctx, PreferenceStore store, PreferenceSet set,
																 ChangeableCondition visibility) {
		super.contributeSettings(ctx, store, set, visibility);
		getPreferenceStore().addBroadcastListener(this);
		MainActivityPrefs.get().addBroadcastListener(this);
		FermataApplication.get().getPreferenceStore().addBroadcastListener(this);

		set.addBooleanPref(o -> {
			o.store = getPreferenceStore();
			o.pref = YT_OPEN_ON_START;
			o.title = R.string.open_on_start;
			o.visibility = visibility;
		});
		set.addBooleanPref(o -> {
			o.store = getPreferenceStore();
			o.pref = YT_AUTO_HIGHEST_QUALITY;
			o.title = R.string.auto_highest_video_quality;
			o.visibility = visibility;
		});

		if (AUTO) {
			set.addBooleanPref(o -> {
				o.store = getPreferenceStore();
				o.pref = YT_SKIP_ADD;
				o.title = R.string.try_to_skip_ad;
				o.visibility = visibility;
			});
		}

		YoutubeSponsorBlock.contributeSettings(getPreferenceStore(), set, visibility);
	}

	@Override
	public void onPreferenceChanged(PreferenceStore store, List<Pref<?>> prefs) {
		if (ignorePrefChange) return;
		ignorePrefChange = true;

		if (prefs.contains(getInfo().enabledPref)) {
			if (!store.getBooleanPref(getInfo().enabledPref)) {
				MainActivityPrefs ap = MainActivityPrefs.get();
				getPreferenceStore().applyBooleanPref(YT_OPEN_ON_START, false);
				if (getInfo().className.equals(ap.getShowAddonOnStartPref()))
					ap.setShowAddonOnStartPref(null);
			}
		} else if (prefs.contains(YT_OPEN_ON_START)) {
			MainActivityPrefs ap = MainActivityPrefs.get();
			if (store.getBooleanPref(YT_OPEN_ON_START)) {
				ap.setShowAddonOnStartPref(getInfo().className);
			} else if (getInfo().className.equals(ap.getShowAddonOnStartPref())) {
				ap.setShowAddonOnStartPref(null);
			}
		} else if (prefs.contains(MainActivityPrefs.SHOW_ADDON_ON_START)) {
			getPreferenceStore().applyBooleanPref(YT_OPEN_ON_START,
					getInfo().className.equals(MainActivityPrefs.get().getShowAddonOnStartPref()));
		}

		ignorePrefChange = false;
	}

	@Override
	public void uninstall() {
		getPreferenceStore().removeBroadcastListener(this);
		MainActivityPrefs.get().removeBroadcastListener(this);
		FermataApplication.get().getPreferenceStore().removeBroadcastListener(this);
	}

	VideoScale getScale() {
		switch (getPreferenceStore().getStringPref(VIDEO_SCALE)) {
			case "fill":
				return VideoScale.FILL;
			case "contain":
				return VideoScale.CONTAIN;
			case "cover":
				return VideoScale.COVER;
			case "small":
				return VideoScale.SMALL;
			case "tiny":
				return VideoScale.TINY;
			default:
				return VideoScale.NONE;
		}
	}

	void setScale(VideoScale scale) {
		getPreferenceStore().applyStringPref(VIDEO_SCALE, scale.prefName());
	}

	boolean autoHighestQuality() {
		return getPreferenceStore().getBooleanPref(YT_AUTO_HIGHEST_QUALITY);
	}

	boolean autoHighestQualityChanged(List<Pref<?>> prefs) {
		return prefs.contains(YT_AUTO_HIGHEST_QUALITY);
	}

	enum VideoScale {
		FILL("object-fit:fill", 1f),
		CONTAIN("object-fit:contain", 1f),
		COVER("object-fit:cover", 1f),
		NONE("object-fit:none", 1f),
		SMALL("object-fit:contain", 0.5f),
		TINY("object-fit:contain", 0.33f);

		private final String style;
		private final float factor;

		VideoScale(String style, float factor) {
			this.style = style;
			this.factor = factor;
		}

		String prefName() {
			return name().toLowerCase();
		}

		String cssStyle(int position) {
			if (factor == 1f) return style;

			String transform = "scale(" + factor + ")";
			return switch (position) {
				case VIDEO_POSITION_TOP_LEFT -> smallStyle("top:0;left:0", "top left", transform);
				case VIDEO_POSITION_TOP -> smallStyle("top:0;left:50%", "top center",
						"translateX(-50%) " + transform);
				case VIDEO_POSITION_TOP_RIGHT -> smallStyle("top:0;right:0", "top right", transform);
				case VIDEO_POSITION_LEFT -> smallStyle("top:50%;left:0", "center left",
						"translateY(-50%) " + transform);
				case VIDEO_POSITION_RIGHT -> smallStyle("top:50%;right:0", "center right",
						"translateY(-50%) " + transform);
				case VIDEO_POSITION_BOTTOM_LEFT -> smallStyle("bottom:0;left:0", "bottom left", transform);
				case VIDEO_POSITION_BOTTOM -> smallStyle("bottom:0;left:50%", "bottom center",
						"translateX(-50%) " + transform);
				case VIDEO_POSITION_BOTTOM_RIGHT -> smallStyle("bottom:0;right:0", "bottom right", transform);
				default -> smallStyle("top:50%;left:50%", "center center",
						"translate(-50%,-50%) " + transform);
			};
		}

		private String smallStyle(String position, String origin, String transform) {
			return style + ";position:absolute;width:100%;height:100%;margin:0;" + position +
					";transform:" + transform + ";transform-origin:" + origin;
		}

	}
}
