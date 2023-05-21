/*
 * This is the source code of Wallet for Android v. 1.0.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright Nikolai Kudashov, 2019-2020.
 */

package org.UI.Fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.camera.CameraSession;
import org.telegram.messenger.camera.CameraView;
import org.telegram.messenger.camera.Size;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.Utils.Callbacks;

import java.nio.ByteBuffer;

public class CameraScanActivity extends BaseFragment implements Camera.PreviewCallback {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 134;
    private static final int GET_IMAGE_FROM_GALLERY_REQUEST_CODE = 11;

    private final HandlerThread backgroundHandlerThread = new HandlerThread("ScanCamera");
    private final BarcodeDetector visionQrReader;
    private final QRCodeReader qrReader;

    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint = new Paint();
    private final Path path = new Path();

    private TextView titleTextView;
    private CameraView cameraView;
    private Handler handler;
    private TextView recognizedMrzView;
    private ImageView galleryButton;
    private ImageView flashButton;
    private AnimatorSet flashAnimator;

    private LinearLayout noAccessLayout;
    private TextView noAccessButton;

    private Callbacks.StringCallback delegate;
    private boolean recognized;
    private boolean hasCameraPermission;
    private boolean cameraInited;

    public CameraScanActivity () {
        super();
        qrReader = new QRCodeReader();
        visionQrReader = new BarcodeDetector.Builder(ApplicationLoader.applicationContext).setBarcodeFormats(Barcode.QR_CODE).build();
    }

    private void initCamera () {
        if (cameraInited || !hasCameraPermission) return;
        CameraController.getInstance().initCamera(() -> {
            cameraInited = true;
            if (cameraView != null) {
                cameraView.initCamera();
            }
        });
    }

    @Override
    public View createView (Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
        actionBar.setCastShadows(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        paint.setColor(0x7f000000);
        cornerPaint.setColor(0xffffffff);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(AndroidUtilities.dp(3.5f));
        cornerPaint.setStrokeJoin(Paint.Join.ROUND);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        ViewGroup viewGroup = new ViewGroup(context) {
            final RectF tmpRect = new RectF();

            @Override
            protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);
                actionBar.measure(widthMeasureSpec, heightMeasureSpec);
                cameraView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                recognizedMrzView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                galleryButton.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY));
                flashButton.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY));
                titleTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                noAccessLayout.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                noAccessButton.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(200), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
                setMeasuredDimension(width, height);
            }

            @Override
            protected void onLayout (boolean changed, int l, int t, int r, int b) {
                int x, y;
                actionBar.layout(0, 0, actionBar.getMeasuredWidth(), actionBar.getMeasuredHeight());
                cameraView.layout(0, 0, cameraView.getMeasuredWidth(), cameraView.getMeasuredHeight());
                int size = (int) (Math.min(cameraView.getWidth(), cameraView.getHeight()) / 1.5f);
                y = (cameraView.getMeasuredHeight() - size) / 2 - titleTextView.getMeasuredHeight() - AndroidUtilities.dp(28);
                titleTextView.layout(0, y, titleTextView.getMeasuredWidth(), y + titleTextView.getMeasuredHeight());
                recognizedMrzView.layout(0, getMeasuredHeight() - recognizedMrzView.getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight());

                x = cameraView.getMeasuredWidth() / 2 + AndroidUtilities.dp(36);
                y = (cameraView.getMeasuredHeight() - size) / 2 + size + AndroidUtilities.dp(47);
                flashButton.layout(x, y, x + flashButton.getMeasuredWidth(), y + flashButton.getMeasuredHeight());

                x = cameraView.getMeasuredWidth() / 2 - AndroidUtilities.dp(36) - galleryButton.getMeasuredWidth();
                galleryButton.layout(x, y, x + galleryButton.getMeasuredWidth(), y + galleryButton.getMeasuredHeight());

                y = t + actionBar.getMeasuredHeight() + AndroidUtilities.dp(146);
                noAccessLayout.layout(l, y, r, y + noAccessLayout.getMeasuredHeight());

                x = cameraView.getMeasuredWidth() / 2 - AndroidUtilities.dp(100);
                y = cameraView.getMeasuredHeight() - AndroidUtilities.dp(148);
                noAccessButton.layout(x, y, x + noAccessButton.getMeasuredWidth(), y + noAccessButton.getMeasuredHeight());
            }

            @Override
            protected boolean drawChild (Canvas canvas, View child, long drawingTime) {
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (child == cameraView) {
                    int size = (int) ((Math.min(child.getWidth(), child.getHeight()) / 1.5f));
                    int x = (child.getWidth() - size) / 2;
                    int y = (child.getHeight() - size) / 2;
                    canvas.drawRect(0, 0, child.getMeasuredWidth(), y, paint);
                    canvas.drawRect(0, y + size, child.getMeasuredWidth(), child.getMeasuredHeight(), paint);
                    canvas.drawRect(0, y, x, y + size, paint);
                    canvas.drawRect(x + size, y, child.getMeasuredWidth(), y + size, paint);

                    path.reset();
                    path.moveTo(x, y + AndroidUtilities.dp(26));
                    path.lineTo(x, y + AndroidUtilities.dp(6));
                    tmpRect.set(x, y, x + AndroidUtilities.dp(12), y + AndroidUtilities.dp(12));
                    path.arcTo(tmpRect, 180, 90, false);
                    path.lineTo(x + AndroidUtilities.dp(6), y);
                    path.lineTo(x + AndroidUtilities.dp(26), y);
                    tmpRect.set(x, y, x + AndroidUtilities.dp(1), y + AndroidUtilities.dp(1));
                    canvas.drawRect(tmpRect, paint);
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.moveTo(x + size, y + AndroidUtilities.dp(26));
                    path.lineTo(x + size, y + AndroidUtilities.dp(6));
                    tmpRect.set(x + size - AndroidUtilities.dp(12), y, x + size, y + AndroidUtilities.dp(12));
                    path.arcTo(tmpRect, 0, -90, false);
                    path.lineTo(x + size - AndroidUtilities.dp(6), y);
                    path.lineTo(x + size - AndroidUtilities.dp(26), y);
                    tmpRect.set(x + size - AndroidUtilities.dp(1), y, x + size, y + AndroidUtilities.dp(1));
                    canvas.drawRect(tmpRect, paint);
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.moveTo(x, y + size - AndroidUtilities.dp(26));
                    path.lineTo(x, y + size - AndroidUtilities.dp(6));
                    tmpRect.set(x, y + size - AndroidUtilities.dp(12), x + AndroidUtilities.dp(12), y + size);
                    path.arcTo(tmpRect, 180, -90, false);
                    path.lineTo(x + AndroidUtilities.dp(6), y + size);
                    path.lineTo(x + AndroidUtilities.dp(26), y + size);
                    tmpRect.set(x, y + size - AndroidUtilities.dp(1), x + AndroidUtilities.dp(1), y + size);
                    canvas.drawRect(tmpRect, paint);
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.moveTo(x + size, y + size - AndroidUtilities.dp(26));
                    path.lineTo(x + size, y + size - AndroidUtilities.dp(6));
                    tmpRect.set(x + size - AndroidUtilities.dp(12), y + size - AndroidUtilities.dp(12), x + size, y + size);
                    path.arcTo(tmpRect, 0, 90, false);
                    path.lineTo(x + size - AndroidUtilities.dp(6), y + size);
                    path.lineTo(x + size - AndroidUtilities.dp(26), y + size);
                    tmpRect.set(x + size - AndroidUtilities.dp(1), y + size - AndroidUtilities.dp(1), x + size, y + size);
                    canvas.drawRect(tmpRect, paint);
                    canvas.drawPath(path, cornerPaint);
                }
                return result;
            }
        };
        viewGroup.setOnTouchListener((v, event) -> true);
        fragmentView = viewGroup;

        cameraView = new CameraView(context, false);
        cameraView.setUseMaxPreview(true);
        cameraView.setOptimizeForBarcode(true);
        cameraView.setDelegate(new CameraView.CameraViewDelegate() {
            @Override
            public void onCameraCreated (Camera camera) {

            }

            @Override
            public void onCameraInit () {
                startRecognizing();
            }
        });
        viewGroup.addView(cameraView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionBar.setBackgroundDrawable(null);
        actionBar.setAddToContainer(false);
        actionBar.setItemsColor(0xffffffff, false);
        actionBar.setItemsBackgroundColor(0x22ffffff, false);
        viewGroup.setBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackground));
        viewGroup.addView(actionBar);

        titleTextView = new TextView(context);
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        titleTextView.setText(LocaleController.getString("WalletScanCode", R.string.WalletScanCode));
        titleTextView.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        titleTextView.setText(LocaleController.getString("WalletScanCode", R.string.WalletScanCode));
        viewGroup.addView(titleTextView);

        noAccessLayout = new LinearLayout(context);
        noAccessLayout.setOrientation(LinearLayout.VERTICAL);
        viewGroup.addView(noAccessLayout);

        TextView noAccessTitleText = new TextView(context);
        noAccessTitleText.setGravity(Gravity.CENTER_HORIZONTAL);
        noAccessTitleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        noAccessTitleText.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        noAccessTitleText.setText(LocaleController.getString("WalletScanNoAccess", R.string.WalletScanNoAccess));

        noAccessLayout.addView(noAccessTitleText);
        TextView noAccessDescriptionText = new TextView(context);
        noAccessDescriptionText.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        noAccessDescriptionText.setGravity(Gravity.CENTER_HORIZONTAL);
        noAccessDescriptionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        noAccessDescriptionText.setMaxWidth(AndroidUtilities.dp(280));
        noAccessDescriptionText.setText(LocaleController.getString("WalletPermissionNoCamera", R.string.WalletPermissionNoCamera));
        noAccessLayout.addView(noAccessDescriptionText, LayoutHelper.createLinear(280, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        noAccessButton = new TextView(context);
        noAccessButton.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), 0);
        noAccessButton.setGravity(Gravity.CENTER);
        noAccessButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        noAccessButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        noAccessButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        noAccessButton.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_wallet_defaultTonBlue), Theme.getColor(Theme.key_wallet_buttonTonBluePressed)));
        noAccessButton.setMinWidth(AndroidUtilities.dp(200));
        noAccessButton.setText(LocaleController.getString("PermissionOpenSettings", R.string.PermissionOpenSettings));
        noAccessButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                getParentActivity().startActivity(intent);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        viewGroup.addView(noAccessButton);

        recognizedMrzView = new TextView(context);
        recognizedMrzView.setTextColor(0xffffffff);
        recognizedMrzView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        recognizedMrzView.setAlpha(0);

        recognizedMrzView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        recognizedMrzView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        recognizedMrzView.setText(LocaleController.getString("WalletScanCodeNotFound", R.string.WalletScanCodeNotFound));
        viewGroup.addView(recognizedMrzView);

        galleryButton = new ImageView(context);
        galleryButton.setScaleType(ImageView.ScaleType.CENTER);
        galleryButton.setImageResource(R.drawable.qr_gallery);
        galleryButton.setBackgroundDrawable(Theme.createSelectorDrawableFromDrawables(Theme.createCircleDrawable(AndroidUtilities.dp(56), 0x66ffffff), Theme.createCircleDrawable(AndroidUtilities.dp(60), 0x44ffffff)));
        viewGroup.addView(galleryButton);
        galleryButton.setOnClickListener(currentImage -> {
            if (getParentActivity() == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 23) {
                if (getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                    return;
                }
            }
            try {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                getParentActivity().startActivityForResult(photoPickerIntent, GET_IMAGE_FROM_GALLERY_REQUEST_CODE);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });

        flashButton = new ImageView(context);
        flashButton.setScaleType(ImageView.ScaleType.CENTER);
        flashButton.setImageResource(R.drawable.qr_flashlight);
        flashButton.setBackgroundDrawable(Theme.createCircleDrawable(AndroidUtilities.dp(56), 0x66ffffff));
        viewGroup.addView(flashButton);
        flashButton.setOnClickListener(currentImage -> {
            CameraSession session = cameraView.getCameraSession();
            if (session != null) {
                ShapeDrawable shapeDrawable = (ShapeDrawable) flashButton.getBackground();
                if (flashAnimator != null) {
                    flashAnimator.cancel();
                    flashAnimator = null;
                }
                flashAnimator = new AnimatorSet();
                ObjectAnimator animator = ObjectAnimator.ofInt(shapeDrawable, AnimationProperties.SHAPE_DRAWABLE_ALPHA, flashButton.getTag() == null ? 0x88 : 0x66);
                animator.addUpdateListener(animation -> flashButton.invalidate());
                flashAnimator.playTogether(animator);
                flashAnimator.setDuration(200);
                flashAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                flashAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd (Animator animation) {
                        flashAnimator = null;
                    }
                });
                flashAnimator.start();
                if (flashButton.getTag() == null) {
                    flashButton.setTag(1);
                    session.setTorchEnabled(true);
                } else {
                    flashButton.setTag(null);
                    session.setTorchEnabled(false);
                }
            }
        });

        if (getParentActivity() != null) {
            getParentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        fragmentView.setKeepScreenOn(true);

        checkCameraPermission();

        return fragmentView;
    }

    public void setDelegate (Callbacks.StringCallback cameraScanActivityDelegate) {
        delegate = cameraScanActivityDelegate;
    }

    public void destroy (boolean async, final Runnable beforeDestroyRunnable) {
        if (cameraView != null) {
            cameraView.destroy(async, beforeDestroyRunnable);
            cameraView = null;
        }
        backgroundHandlerThread.quitSafely();
    }

    private void startRecognizing () {
        backgroundHandlerThread.start();
        handler = new Handler(backgroundHandlerThread.getLooper());
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run () {
                if (cameraView != null && !recognized && cameraView.getCameraSession() != null) {
                    cameraView.getCameraSession().setOneShotPreviewCallback(CameraScanActivity.this);
                    AndroidUtilities.runOnUIThread(this, 500);
                }
            }
        });
    }

    private void showErrorAlert () {
        AlertsCreator.createSimpleAlert(getParentActivity(), LocaleController.getString("WalletQRCode", R.string.WalletQRCode), LocaleController.getString("WalletScanImageNotFound", R.string.WalletScanImageNotFound)).show();
    }

    private void onNoQrFound (boolean alert) {
        AndroidUtilities.runOnUIThread(() -> {
            if (alert) {
                showErrorAlert();
                return;
            }
            if (recognizedMrzView.getTag() != null) {
                recognizedMrzView.setTag(null);
                recognizedMrzView.animate().setDuration(200).alpha(0.0f).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
            }
        });
    }

    private void onNoWalletFound (boolean alert) {
        AndroidUtilities.runOnUIThread(() -> {
            if (alert) {
                showErrorAlert();
                return;
            }
            if (recognizedMrzView.getTag() == null) {
                recognizedMrzView.setTag(1);
                recognizedMrzView.animate().setDuration(200).alpha(1.0f).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
            }
        });
    }

    @Override
    public void onPreviewFrame (final byte[] data, final Camera camera) {
        handler.post(() -> {
            try {
                Size size = cameraView.getPreviewSize();
                int format = camera.getParameters().getPreviewFormat();
                int side = (int) (Math.min(size.getWidth(), size.getHeight()) / 1.5f);
                int x = (size.getWidth() - side) / 2;
                int y = (size.getHeight() - side) / 2;

                String text = tryReadQr(data, size, x, y, side, null);
                if (text != null) {
                    recognized = true;
                    camera.stopPreview();
                    AndroidUtilities.runOnUIThread(() -> {
                        foundQrText = text;
                        /*if (delegate != null) {
                            delegate.didFindQr(text);
                        }*/
                        finishFragment();
                    });
                }
            } catch (Throwable ignore) {
                onNoQrFound(false);
            }
        });
    }

    private String tryReadQr (byte[] data, Size size, int x, int y, int side, Bitmap bitmap) {
        try {
            String text;
            if (visionQrReader.isOperational()) {
                Frame frame;
                if (bitmap != null) {
                    frame = new Frame.Builder().setBitmap(bitmap).build();
                } else {
                    frame = new Frame.Builder().setImageData(ByteBuffer.wrap(data), size.getWidth(), size.getHeight(), ImageFormat.NV21).build();
                }
                SparseArray<Barcode> codes = visionQrReader.detect(frame);
                if (codes != null && codes.size() > 0) {
                    text = codes.valueAt(0).rawValue;
                } else {
                    text = null;
                }
            } else {
                LuminanceSource source;
                if (bitmap != null) {
                    int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                    bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
                } else {
                    source = new PlanarYUVLuminanceSource(data, size.getWidth(), size.getHeight(), x, y, side, side, false);
                }

                Result result = qrReader.decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
                if (result == null) {
                    onNoQrFound(bitmap != null);
                    return null;
                }
                text = result.getText();
            }
            if (TextUtils.isEmpty(text)) {
                onNoQrFound(bitmap != null);
                return null;
            }
            /* if (!text.startsWith("ton://transfer/")) {
                onNoWalletFound(bitmap != null);
                return null;
            }
            Uri uri = Uri.parse(text);
            String path = uri.getPath().replace("/", "");
            if (!getTonController().isValidWalletAddress(path)) {
                onNoWalletFound(bitmap != null);
                return null;
            } */
            return text;
        } catch (Throwable ignore) {
            onNoQrFound(bitmap != null);
        }
        return null;
    }


    private String foundQrText;

    @Override
    public boolean onFragmentCreate () {
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy () {
        super.onFragmentDestroy();
        destroy(false, null);
        if (getParentActivity() != null) {
            getParentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        if (visionQrReader != null) {
            visionQrReader.release();
        }
        if (delegate != null && foundQrText != null) {
            delegate.run(foundQrText);
        }
    }

    @Override
    public void onResume () {
        super.onResume();
        AndroidUtilities.runOnUIThread(this::checkCameraPermission, 100);
    }

    @Override
    public void onPause () {
        super.onPause();

        // cameraView.destroy(false, null);
        // cameraInited = false;
        // backgroundHandlerThread.quitSafely();
    }

    @Override
    public void onActivityResultFragment (int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GET_IMAGE_FROM_GALLERY_REQUEST_CODE) {
                if (data == null || data.getData() == null) {
                    return;
                }
                try {
                    Point screenSize = AndroidUtilities.getRealScreenSize();
                    Bitmap bitmap = ImageLoader.loadBitmap(null, data.getData(), screenSize.x, screenSize.y, true);
                    String text = tryReadQr(null, null, 0, 0, 0, bitmap);
                    if (text != null) {
                        /*if (delegate != null) {
                            delegate.didFindQr(text);
                        }*/
                        foundQrText = text;
                        finishFragment();
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResultFragment (int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AndroidUtilities.runOnUIThread(this::checkCameraPermission, 150);
            }
        }
    }

    private void checkCameraPermission () {
        if (getParentActivity() == null) return;
        if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            hasCameraPermission = false;
        } else {
            hasCameraPermission = true;
        }

        cameraView.setVisibility(hasCameraPermission ? View.VISIBLE : View.GONE);
        galleryButton.setVisibility(hasCameraPermission ? View.VISIBLE : View.GONE);
        flashButton.setVisibility(hasCameraPermission ? View.VISIBLE : View.GONE);
        titleTextView.setVisibility(hasCameraPermission ? View.VISIBLE : View.GONE);
        noAccessLayout.setVisibility(!hasCameraPermission ? View.VISIBLE : View.GONE);
        noAccessButton.setVisibility(!hasCameraPermission ? View.VISIBLE : View.GONE);
        initCamera();
    }
}
