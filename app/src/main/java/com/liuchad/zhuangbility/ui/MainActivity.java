package com.liuchad.zhuangbility.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.liuchad.zhuangbility.Constants;
import com.liuchad.zhuangbility.Mode;
import com.liuchad.zhuangbility.R;
import com.liuchad.zhuangbility.base.BaseActivity;
import com.liuchad.zhuangbility.event.MultiPicSelectedEvent;
import com.liuchad.zhuangbility.event.SelectPicEvent;
import com.liuchad.zhuangbility.event.SinglePicSelectedEvent;
import com.liuchad.zhuangbility.util.CommonUtils;
import com.liuchad.zhuangbility.widget.IconView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import butterknife.BindView;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import in.workarounds.bundler.Bundler;
import in.workarounds.bundler.annotations.RequireBundler;
import me.priyesh.chroma.ChromaDialog;
import me.priyesh.chroma.ColorMode;
import me.priyesh.chroma.ColorSelectListener;

@RequireBundler
public class MainActivity extends BaseActivity
    implements SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener,
    IconView.IconClickListener {

    public final int REQUEST_SELECT_PIC = 101;
    private static final int REQUEST_CODE_CAMERA_CROP = 103;

    private static final int[] defaultFontColors = new int[] {
        Color.parseColor("#333333"),    /*默认字体颜色*/
        Color.BLACK, Color.WHITE, Color.GRAY,
    };
    @BindView(R.id.toolbar) Toolbar toolbar;

    @BindView(R.id.zhuangbi) ImageView mEmoji;
    @BindView(R.id.emoji_slogan) EditText mEmojiInputContent;
    @BindView(R.id.text_size_progress) SeekBar mTextSizeProgress;
    @BindView(R.id.text_mask_progress) SeekBar mTextMaskProgress;
    @BindView(R.id.text_direction_bottom) RadioButton mTextDirectionBottom;
    @BindView(R.id.text_direction_up) RadioButton mTextDirectionUp;
    @BindView(R.id.text_inside_pic) RadioButton mTextInsidePic;
    @BindView(R.id.text_beyond_pic) RadioButton mTextBeyondPic;
    @BindView(R.id.higher_quality) RadioButton mHigherQuality;
    @BindView(R.id.lower_quality) RadioButton mLowerQuality;
    @BindView(R.id.pure_text) RadioButton mPureText;
    @BindView(R.id.bold) CheckBox mBold;
    @BindView(R.id.italic) CheckBox mItalic;
    @BindView(R.id.black) RadioButton mBlack;
    @BindView(R.id.white) RadioButton mWhite;
    @BindView(R.id.gray) RadioButton mGray;
    @BindView(R.id.default_color) RadioButton mDefaultColor;
    @BindView(R.id.color_picker) ImageView mColorPicker;
    @BindView(R.id.text_color_rg) RadioGroup mTextColorRg;
    @BindView(R.id.tips_quality) Button mTipsQuality;
    @BindView(R.id.share_to_qq) IconView mShareToQQ;
    @BindView(R.id.share_to_wechat) IconView mShareToWeChat;
    @BindView(R.id.save_to_local) IconView mSaveToLocal;
    @BindView(R.id.select_from_galery) IconView mSelectFromGalery;
    @BindView(R.id.select_from_recomend) IconView mSelectFromRecomend;
    @BindView(R.id.options_container) LinearLayout mOptionContainer;

    /** 要增加的文字 */
    String mEmojiText = "";

    private Bitmap mBitmapFromFile;

    /** 原图的Bitmap */
    @SuppressWarnings("FieldCanBeLocal") private Bitmap mOriginalEmoji;

    /** 修改之后的图片的Bitmap */
    private Bitmap mComposedEmoji;

    private boolean mIsTextInside = false;
    private boolean mIsTextBottom = true;

    /** 默认的文字晕影值 */
    private int mMaskValue = 3;

    private int mTextSize = 40;

    /** 选择颜色值的下标 */
    private int mTextPaintColorIndex = 0;

    /** 颜色选择器设置的颜色值 */
    private int mColorPicked = -1;

    private int mFontStyle = 0;
    private TextPaint mTextPaint;
    private Paint mRectPaint;
    private Canvas mCanvas;

    /** 默认的素材图片资源id */
    private int mDefaultEmojiId = R.drawable.kt2;

    /** (高清/祖传)模式标志位 */
    private int mPicMode = QuantityMode.HIGH;

    private int screenWidth;

    private String[] modeStringArray = { Constants.GAOQING, Constants.ZUCHUAN, Constants.PURE_TEXT };

    private TextWatcher mContentTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            mEmojiText = s.toString();
            doInvalidateCanvas();
        }

        @Override public void afterTextChanged(Editable s) {
        }
    };

    @Override protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override protected void initInjector() {
        Bundler.inject(this);
        ShareSDK.initSDK(MainActivity.this, Constants.MOB_KEY);
    }

    private void showShare() {
        ShareSDK.initSDK(this);
        OnekeyShare oks = new OnekeyShare();
//关闭sso授权
        oks.disableSSOWhenAuthorize();

// title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间等使用
        oks.setTitle("标题");
// titleUrl是标题的网络链接，QQ和QQ空间等使用
        oks.setTitleUrl("http://sharesdk.cn");
// text是分享文本，所有平台都需要这个字段
        oks.setText("我是分享文本");
// imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
//oks.setImagePath("/sdcard/test.jpg");//确保SDcard下面存在此张图片
// url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl("http://sharesdk.cn");
// comment是我对这条分享的评论，仅在人人网和QQ空间使用
        oks.setComment("我是测试评论文本");
// site是分享此内容的网站名称，仅在QQ空间使用
        oks.setSite(getString(R.string.app_name));
// siteUrl是分享此内容的网站地址，仅在QQ空间使用
        oks.setSiteUrl("http://sharesdk.cn");

// 启动分享GUI
        oks.show(this);
    }

    @Override protected void initView() {
        toolbar.setBackgroundColor(getResources().getColor(R.color.theme_light));
        setSupportActionBar(toolbar);

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mRectPaint = new Paint();
        mCanvas = new Canvas();
        mTextSizeProgress.setMax((int) (getResources().getDisplayMetrics().density * 50));
        mTextSizeProgress.setOnSeekBarChangeListener(this);
        mTextMaskProgress.setOnSeekBarChangeListener(this);
        mEmojiInputContent.addTextChangedListener(mContentTextWatcher);
        mDefaultColor.setOnCheckedChangeListener(this);
        mBlack.setOnCheckedChangeListener(this);
        mWhite.setOnCheckedChangeListener(this);
        mGray.setOnCheckedChangeListener(this);
        mBold.setOnCheckedChangeListener(this);
        mItalic.setOnCheckedChangeListener(this);
        mTextInsidePic.setOnCheckedChangeListener(this);
        mTextBeyondPic.setOnCheckedChangeListener(this);
        mTextDirectionBottom.setOnCheckedChangeListener(this);
        mTextDirectionUp.setOnCheckedChangeListener(this);
        mColorPicker.setOnClickListener(this);
        mTipsQuality.setOnClickListener(this);
        mHigherQuality.setOnCheckedChangeListener(this);
        mLowerQuality.setOnCheckedChangeListener(this);
        mPureText.setOnCheckedChangeListener(this);
        mShareToQQ.setIconClickListener(this);
        mShareToWeChat.setIconClickListener(this);
        mSelectFromGalery.setIconClickListener(this);
        mSaveToLocal.setIconClickListener(this);
        mSelectFromRecomend.setIconClickListener(this);
    }

    @Override protected void initData() {
        EventBus.getDefault().register(this);
        screenWidth = CommonUtils.getScreenWidth(MainActivity.this);
    }

    @Override public void reportFullyDrawn() {
        super.reportFullyDrawn();
    }

    private void doInvalidateCanvas() { /*输入文字的总宽度*/
        float textTotalWidth = mTextPaint.measureText(mEmojiText);
        if (mBitmapFromFile == null)

        {
            mOriginalEmoji = BitmapFactory.decodeResource(getResources(), mDefaultEmojiId);
        } else {
            mOriginalEmoji = mBitmapFromFile;
        }
        //输入文字的总高度(包括换行)
        int extraTextAreaHeight =
            ((int) Math.ceil(textTotalWidth / mOriginalEmoji.getWidth())) * (int) ((mTextSize) * 1.2);

        Typeface font = Typeface.create(Typeface.SANS_SERIF, mFontStyle);

        mTextPaint.setTypeface(font);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mColorPicked == -1 ? defaultFontColors[mTextPaintColorIndex] : mColorPicked);
        mTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mTextPaint.setMaskFilter(new BlurMaskFilter(mMaskValue, BlurMaskFilter.Blur.SOLID));

        mRectPaint.setColor(Color.WHITE);
        mRectPaint.setAntiAlias(true);
        mRectPaint.setStyle(Paint.Style.FILL);

        mComposedEmoji = Bitmap.createBitmap(
            mOriginalEmoji.getWidth(),
            mIsTextInside ? mOriginalEmoji.getHeight() : mOriginalEmoji.getHeight() + extraTextAreaHeight,
            Bitmap.Config.ARGB_8888);

        resizeImageView(mComposedEmoji.getWidth(), mComposedEmoji.getHeight());

        mCanvas.setBitmap(mComposedEmoji);

        //通过StaticLayout来达到换行的效果.
        StaticLayout staticLayout = new StaticLayout(mEmojiText, mTextPaint, mOriginalEmoji.getWidth(),
            Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        mCanvas.save();
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        if (!mIsTextInside) {
            if (mIsTextBottom) {
                mCanvas.drawBitmap(mOriginalEmoji, 0f, 0f, null);
                mCanvas.drawRect(0, mOriginalEmoji.getHeight(), mOriginalEmoji.getWidth(),
                    mOriginalEmoji.getHeight() + extraTextAreaHeight,
                    mRectPaint);
                mCanvas.translate(0, mOriginalEmoji.getHeight());
            } else {
                mCanvas.drawBitmap(mOriginalEmoji, 0f, extraTextAreaHeight, null);
                mCanvas.drawRect(0, 0, mOriginalEmoji.getWidth(), extraTextAreaHeight, mRectPaint);
            }
        } else {
            mCanvas.drawBitmap(mOriginalEmoji, 0f, 0f, null);
            if (mIsTextBottom) {
                mCanvas.translate(0, mOriginalEmoji.getHeight() - extraTextAreaHeight);
            }
        }

        staticLayout.draw(mCanvas);
        mCanvas.restore();
        mEmoji.setImageBitmap(mComposedEmoji);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_SELECT_PIC:

                break;
            case REQUEST_CODE_CAMERA_CROP: //裁剪
                if (resultCode != RESULT_OK) {
                    return;
                }
                String cameraPicPath = CommonUtils.SDCARD + "/" + "temp.jpg";
                cropImage(cameraPicPath);
                break;
        }
    }

    @Subscribe
    public void onSinglePicSelectedEvent(SinglePicSelectedEvent event) {
        if (TextUtils.isEmpty(event.path)) {
            return;
        }

        Uri uri = Uri.fromFile(new File(event.path));
        CommonUtils.startPhotoZoom(MainActivity.this, uri, REQUEST_CODE_CAMERA_CROP);
    }

    @Subscribe
    public void onMultiPicSelectedEvent(MultiPicSelectedEvent event) {

    }

    private void cropImage(String cameraPicPath) {
        mBitmapFromFile = BitmapFactory.decodeFile(cameraPicPath);
        if (mBitmapFromFile.getWidth() > screenWidth) {
            double rate = (double) screenWidth / (double) mBitmapFromFile.getWidth();
            int newWidth = (int) (rate * mBitmapFromFile.getWidth());
            int newHeight = (int) (rate * mBitmapFromFile.getHeight());
            mBitmapFromFile.recycle();
            mBitmapFromFile = CommonUtils.decodeSampledBitmapFromFile(cameraPicPath, newWidth, newHeight);
        }

        resizeImageView(mBitmapFromFile.getWidth(), mBitmapFromFile.getHeight());
        doInvalidateCanvas();
        boolean deleted = new File(cameraPicPath).delete();//删除拍照的图片
        if (!deleted) {
            Log.e("文件删除失败: ", "cameraPicPath " + cameraPicPath);
        }
    }

    public void resizeImageView(int width, int height) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mEmoji.getLayoutParams();
        params.width = width;
        params.height = height;
        mEmoji.setLayoutParams(params);
    }

    public void refreshColorRadioButtonsCheck(int index) {
        mTextPaintColorIndex = index;
        doInvalidateCanvas();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.text_size_progress:
                mTextSize = progress;
                if (!TextUtils.isEmpty(mEmojiText)) {
                    doInvalidateCanvas();
                }
                break;
            case R.id.text_mask_progress:
                mMaskValue = progress;
                if (mMaskValue <= 0) {
                    mMaskValue = 1;
                }
                if (!TextUtils.isEmpty(mEmojiText)) {
                    doInvalidateCanvas();
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.default_color:
                if (isChecked) {
                    refreshColorRadioButtonsCheck(0);
                }
                mColorPicked = -1;
                break;

            case R.id.black:
                if (isChecked) {
                    refreshColorRadioButtonsCheck(1);
                }
                mColorPicked = -1;
                break;

            case R.id.white:
                if (isChecked) {
                    refreshColorRadioButtonsCheck(2);
                }
                mColorPicked = -1;
                break;

            case R.id.gray:
                if (isChecked) {
                    refreshColorRadioButtonsCheck(3);
                }
                mColorPicked = -1;
                break;

            case R.id.text_direction_bottom:
                if (isChecked) {
                    mIsTextBottom = true;
                    doInvalidateCanvas();
                }
                break;

            case R.id.text_direction_up:
                if (isChecked) {
                    mIsTextBottom = false;
                    doInvalidateCanvas();
                }
                break;

            case R.id.text_inside_pic:
                if (isChecked) {
                    mIsTextInside = true;
                    doInvalidateCanvas();
                }
                break;

            case R.id.text_beyond_pic:
                if (isChecked) {
                    mIsTextInside = false;
                    doInvalidateCanvas();
                }
                break;
            case R.id.higher_quality:
                if (isChecked) {
                    mPicMode = QuantityMode.HIGH;
                }
                break;

            case R.id.lower_quality:
                if (isChecked) {
                    mPicMode = QuantityMode.LOW;
                }
                break;
            case R.id.pure_text:
                if (isChecked) {
                    mPicMode = QuantityMode.PURE_TEXT;
                }
                break;

            case R.id.bold:
                if (isChecked) {
                    if (mItalic.isChecked()) {
                        mFontStyle = Typeface.BOLD_ITALIC;
                        doInvalidateCanvas();
                    } else {
                        mFontStyle = Typeface.BOLD;
                        doInvalidateCanvas();
                    }
                } else {
                    if (mItalic.isChecked()) {
                        mFontStyle = Typeface.ITALIC;
                        doInvalidateCanvas();
                    } else {
                        mFontStyle = Typeface.NORMAL;
                        doInvalidateCanvas();
                    }
                }
                break;

            case R.id.italic:
                mFontStyle = Typeface.ITALIC;
                if (isChecked) {
                    if (mBold.isChecked()) {
                        mFontStyle = Typeface.BOLD_ITALIC;
                        doInvalidateCanvas();
                    } else {
                        mFontStyle = Typeface.ITALIC;
                        doInvalidateCanvas();
                    }
                } else {
                    if (mBold.isChecked()) {
                        mFontStyle = Typeface.BOLD;
                        doInvalidateCanvas();
                    } else {
                        mFontStyle = Typeface.NORMAL;
                        doInvalidateCanvas();
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 选择内置图片之后的回调
     *
     * @param event EventBus事件
     */
    @Subscribe
    public void onEvent(SelectPicEvent event) {
        mDefaultEmojiId = event.id;
        mBitmapFromFile = null;
        doInvalidateCanvas();
    }

    @SuppressLint("InflateParams") @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.color_picker:
                //noinspection deprecation
                new ChromaDialog.Builder()
                    .initialColor(getResources().getColor(R.color.theme_light))
                    .colorMode(ColorMode.ARGB)
                    .onColorSelected(new ColorSelectListener() {
                        @Override
                        public void onColorSelected(@ColorInt int i) {
                            mColorPicked = i;
                            doInvalidateCanvas();
                            mTextColorRg.clearCheck();
                        }
                    })
                    .create()
                    .show(getSupportFragmentManager(), Constants.ZHUANGBILITY);
                break;
            case R.id.tips_quality:
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.quality_title))
                    .setView(getLayoutInflater().inflate(R.layout.dialog_quality, null))
                    .show();
                break;
        }
    }

    @Override
    public void onIconClick(IconView view) {
        switch (view.getId()) {
            case R.id.share_to_qq:
                if (CommonUtils.getDeviceModel().contains("mi")) {
                    //部分机型无法直接分享图片文件,所以调用这个方法来分享Bitmap
                    CommonUtils.shareImage(MainActivity.this, mComposedEmoji, Constants.QQ_PACKAGE_NAME);
                } else {
                    shareToFriends(Constants.QQ_SEND_ACTIVITY, Constants.QQ_PACKAGE_NAME);
                }
                break;
            case R.id.share_to_wechat:
                if (CommonUtils.getDeviceModel().contains("mi")) {
                    //部分机型无法直接分享图片文件,所以调用这个方法来分享Bitmap
                    CommonUtils.shareImage(MainActivity.this, mComposedEmoji, Constants.WECHAT_PACKAGE_NAME);
                } else {
                    shareToFriends(Constants.WECHAT_SEND_ACTIVITY, Constants.WECHAT_PACKAGE_NAME);
                }
                break;
            case R.id.select_from_galery:
                Bundler.multiImageSelectorActivity(Mode.MODE_SINGLE, true).start(MainActivity.this);
                break;
            case R.id.select_from_recomend:
                Bundler.selectPicActivity().start(MainActivity.this);
                break;
        }
    }

    private void shareToFriends(String destActivity, String packageName) {
        if (!CommonUtils.isAppInstalled(MainActivity.this, packageName)) {
            handleNotInstalledCase(packageName);
            return;
        }
        Intent sendIntent = new Intent(Constants.ACTION_SEND);
        sendIntent.setType("image/*");
        String qqFilename = getFormatFileName(mPicMode);
        File zhuangbiDir =
            new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator
                + Constants.ZHUANGBILITY);
        if (mComposedEmoji == null) {
            return;
        }
        saveNewEmojiToSdCard(qqFilename, mComposedEmoji);
        File file = new File(zhuangbiDir, qqFilename);
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(MainActivity.this,
                Constants.COM_LIUCHAD_ZHUANGBILITY_FILEPROVIDER, file);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }

        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.setComponent(new ComponentName(packageName, destActivity));
        startActivity(sendIntent);
    }

    private String getFormatFileName(int mode) {
        return getString(R.string.app_name_english)
            + "-"
            + modeStringArray[mode]
            + "-"
            + System.currentTimeMillis()
            + ".jpeg";
    }

    private void handleNotInstalledCase(String packageName) {
        switch (packageName) {
            case Constants.QQ_PACKAGE_NAME:
                CommonUtils.showToast(getString(R.string.have_not_install_qq));
                break;
            case Constants.WECHAT_PACKAGE_NAME:
                CommonUtils.showToast(getString(R.string.have_not_install_wechat));
                break;
        }
    }

    public void saveNewEmojiToSdCard(String filename, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        String snackText = getString(R.string.filename) + filename;
        FileOutputStream out = null;
        File dest = null;
        File zhuangbiDir = new File(
            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Constants.ZHUANGBILITY);
        boolean success = true;
        if (!zhuangbiDir.exists()) {
            success = zhuangbiDir.mkdir();
        }

        if (success) {
            dest = new File(zhuangbiDir, filename);
        }
        if (dest == null) {
            CommonUtils.showToast(getString(R.string.make_fail));
            return;
        }
        try {
            out = new FileOutputStream(dest.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            CommonUtils.showToast(getString(R.string.file_not_found));
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, mPicMode != QuantityMode.LOW ? 100 : 0, out);
        Snackbar.make(mOptionContainer, snackText, Snackbar.LENGTH_LONG)
            .setAction("Action", null)
            .show();
        CommonUtils.refreshLocalDb(MainActivity.this, dest);

        if (!dest.exists()) {
            return;
        }
        Bitmap reusedBitmap = BitmapFactory.decodeFile(dest.getAbsolutePath());
        if (reusedBitmap != null) {
            mEmoji.setImageBitmap(null);
            mEmoji.setImageBitmap(reusedBitmap);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @IntDef({ QuantityMode.HIGH, QuantityMode.LOW, QuantityMode.PURE_TEXT }) @Retention(RetentionPolicy.RUNTIME)
    public @interface QuantityMode {
        int HIGH = 0;
        int LOW = 1;
        int PURE_TEXT = 2;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_theme:
                Bundler.themeActivity().start(MainActivity.this);
                break;
            case R.id.action_save:
                String filename = getFormatFileName(mPicMode);
                if (mComposedEmoji != null) {
                    saveNewEmojiToSdCard(filename, mComposedEmoji);
                }
                break;
            case R.id.action_share:
                showShare();
                break;
            case R.id.action_about:
                Bundler.aboutActivity().start(MainActivity.this);
                break;
            case R.id.action_donate:
                Bundler.donateActivity().start(MainActivity.this);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}



