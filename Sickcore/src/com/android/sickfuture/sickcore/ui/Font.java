//package com.android.sickfuture.sickcore.ui;
//
//import java.util.Map;
//
//import com.google.common.collect.Maps;
//
//import static com.google.common.base.Preconditions.checkNotNull;
//
//import android.R;
//import android.app.Activity;
//import android.content.Context;
//import android.content.res.TypedArray;
//import android.graphics.Typeface;
//import android.support.v4.app.FragmentActivity;
//import android.util.AttributeSet;
//import android.util.Log;
//import android.view.InflateException;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.TextView;
//
///**
// * Provides an ability to apply custom font to all {@link TextView} and subclasses.
// *
// * To install custom font use method {@link #install(Activity)} in {@link Activity#onCreate(android.os.Bundle)}
// * <b>before</b> calling super.onCreate(Bundle).
// *
// * <p/>Example of usage:
// * <pre>
// * {@code
// * public class BaseActivity extends SherlockFragmentActivity {
// *
// *      protected void onCreate(Bundle state) {
// *          applyCustomFontForPreICS();
// *          super.onCreate(state);
// *      }
// *
// *      private void applyCustomFontForPreICS() {
// *          boolean isPreICS = Build.VERSION.SDK_INT < BUILD_VERSION_CODE_ICE_CREAM_SANDWICH
// *          if (isPreICS) {
// *              new Font(
// *                  "font/roboto_regular.ttf",
// *                  "font/roboto_bold.ttf",
// *                  "font/roboto_italic.ttf",
// *                  "font/roboto_bold_italic.ttf"
// *              ).install(this);
// *          }
// *      }
// * }
// * }
// * </pre>
// * @author Alexey Danilov (danikula@gmail.com)
// */
//public class Font {
//
//	private static final String LOG_TAG = Font.class.getSimpleName();
//
//    private static final Map<String, Typeface> FONTS = Maps.newHashMap();
//
//    private String regularFontPath;
//    private String boldFontPath;
//    private String italicFontPath;
//    private String boldItalicFontPath;
//
//    /**
//     * Creates instance to be used for setting particular font.
//     *
//     * @param regularPath regular font assets path, must be not {@code null}
//     * @param boldPath bold font assets path, must be not {@code null}
//     * @param italicPath italic font assets path, must be not {@code null}
//     * @param boldItalicPath bold and italic font assets path, must be not {@code null}
//     */
//    public Font(String regularPath, String boldPath, String italicPath, String boldItalicPath) {
//        this.regularFontPath = checkNotNull(regularPath);
//        this.boldFontPath = checkNotNull(boldPath);
//        this.italicFontPath = checkNotNull(italicPath);
//        this.boldItalicFontPath = checkNotNull(boldItalicPath);
//    }
//
//    /**
//     * Installs custom font to activity.
//     *
//     * @param activity an activity custom font will be installed to, must be not {@code null}.
//     */
//    public void install(Activity activity) {
//        checkNotNull(activity, "Activity must be not null!");
//
//        LayoutInflater layoutInflater = activity.getLayoutInflater();
//        boolean factoryIsEmpty = layoutInflater.getFactory() == null;
//        if (!factoryIsEmpty) {
//            throw new IllegalStateException("Impossible to use this method for this activity: layout factory is set!");
//        }
//        layoutInflater.setFactory(new FontLayoutInflaterFactory());
//    }
//
//    private Typeface getFont(int type, Context context) {
//        switch (type) {
//            case Typeface.NORMAL:
//                return getFont(context, regularFontPath);
//            case Typeface.BOLD:
//                return getFont(context, boldFontPath);
//            case Typeface.ITALIC:
//                return getFont(context, italicFontPath);
//            case Typeface.BOLD_ITALIC:
//                return getFont(context, boldItalicFontPath);
//            default: {
//                throw new IllegalArgumentException("Undefined font type " + type);
//            }
//        }
//    }
//
//    private Typeface getFont(Context context, String path) {
//        if (FONTS.containsKey(path)) {
//            return FONTS.get(path);
//        } else {
//            Typeface typeface = makeTypeface(context, path);
//            FONTS.put(path, typeface);
//            return typeface;
//        }
//    }
//
//    private Typeface makeTypeface(Context context, String path) {
//        try {
//            return Typeface.createFromAsset(context.getAssets(), path);
//        } catch (Exception e) {
//            // add user-friendly error message
//            throw new IllegalArgumentException(String.format("Error creating font from assets path '%s'", path), e);
//        }
//    }
//
//    private void applyFontToTextView(Context context, TextView textView, AttributeSet attrs) {
//        int[] fontStyleAttributes = {R.attr.textStyle};
//        TypedArray typedArray = context.obtainStyledAttributes(attrs, fontStyleAttributes);
//        boolean isStyleSpecified = typedArray.getIndexCount() != 0;
//        int type = isStyleSpecified ? typedArray.getInt(0, Typeface.NORMAL) : Typeface.NORMAL;
//        typedArray.recycle();
//        Typeface font = getFont(type, context);
//        textView.setTypeface(font, type);
//    }
//
//    private final class FontLayoutInflaterFactory implements LayoutInflater.Factory {
//
//        // to improve perfomance the package with the most usable components should be the first.
//        private final String[] ANDROID_UI_COMPONENT_PACKAGES = {
//                "android.widget.",
//                "android.webkit.",
//                "android.view."
//        };
//
//        @Override
//        public View onCreateView(String name, Context context, AttributeSet attrs) {
//            try {
//                // we install custom LayoutInflater.Factory, so FragmentActivity have no chance set own factory and
//                // inflate tag <fragment> in method onCreateView. So  call it explicitly.
//                if ("fragment".equals(name) && context instanceof FragmentActivity) {
//                    FragmentActivity fragmentActivity = (FragmentActivity) context;
//                    return fragmentActivity.onCreateView(name, context, attrs);
//                }
//
//                LayoutInflater layoutInflater = LayoutInflater.from(context);
//
//
//                View view = createView(name, attrs, layoutInflater);
//
//                if (view == null) {
//                    // It's strange! The view is not ours neither android's. May be the package of this view
//                    // is not listed in ANDROID_UI_COMPONENT_PACKAGES. Return null for the default behavior.
//                    Log.d(LOG_TAG, "Cannot create view with name: " + name);
//                    return null;
//                }
//
//                if (view instanceof TextView) {
//                    TextView textView = (TextView) view;
//                    applyFontToTextView(context, textView, attrs);
//                }
//                return view;
//            } catch (InflateException e) {
//                Log.e(LOG_TAG, "Error inflating view", e);
//                return null;
//            } catch (ClassNotFoundException e) {
//                Log.e(LOG_TAG, "Error inflating view", e);
//                return null;
//            }
//        }
//
//        private View createView(String name, AttributeSet attrs, LayoutInflater layoutInflater) throws ClassNotFoundException {
//            View view = null;
//            boolean isAndroidComponent = name.indexOf('.') == -1;
//            if (isAndroidComponent) {
//                // We don't know package name of the view with the given simple name. Try android ui packages listed in
//                // ANDROID_UI_COMPONENT_PACKAGES
//
//                // The same implementation is in the class PhoneLayoutInflater from internal API
//                for (String androidPackage : ANDROID_UI_COMPONENT_PACKAGES) {
//                    try {
//                        view = layoutInflater.createView(name, androidPackage, attrs);
//                        if (view != null) {
//                            break;
//                        }
//                    } catch (ClassNotFoundException e) {
//                        // Do nothing, we will try another package
//                    }
//                }
//            } else {
//                view = layoutInflater.createView(name, null, attrs);
//            }
//            return view;
//        }
//    }
//}