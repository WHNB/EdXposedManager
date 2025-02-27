package com.solohsu.android.edxp.manager.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.solohsu.android.edxp.manager.util.Utils;
import com.android.system.manager.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.ThemeUtil;

import static de.robv.android.xposed.installer.util.ModuleUtil.XP_META_DATA_MODULE;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    protected final Context context;
    private final ApplicationInfo.DisplayNameComparator displayNameComparator;
    private List<ApplicationInfo> fullList, showList;
    private DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private List<String> checkedList;
    private PackageManager pm;
    private ApplicationFilter filter;
    private Callback callback;
    private Comparator<ApplicationInfo> cmp;

    AppAdapter(Context context) {
        this.context = context;
        fullList = showList = Collections.emptyList();
        checkedList = Collections.emptyList();
        filter = new ApplicationFilter();
        pm = context.getPackageManager();
        displayNameComparator = new ApplicationInfo.DisplayNameComparator(pm);
        cmp = displayNameComparator;
        refresh();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false);
        return new ViewHolder(v);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void loadApps() {
        fullList = pm.getInstalledApplications(0);
        if (!XposedApp.getPreferences().getBoolean("show_modules", true)) {
            List<ApplicationInfo> rmList = new ArrayList<>();
            for (ApplicationInfo info : fullList) {
                if (info.metaData != null && info.metaData.containsKey(XP_META_DATA_MODULE)) {
                    rmList.add(info);
                }
            }
            if (rmList.size() > 0) {
                fullList.removeAll(rmList);
            }
        }
        AppHelper.makeSurePath();
        checkedList = generateCheckedList();
        sortApps();
        if (callback != null) {
            callback.onDataReady();
        }
    }

    /**
     * Called during {@link #loadApps()} in non-UI thread.
     *
     * @return list of package names which should be checked when shown
     */
    protected List<String> generateCheckedList() {
        return Collections.emptyList();
    }

    private void sortApps() {
        switch (XposedApp.getPreferences().getInt("list_sort", 0)) {
            case 7:
                cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).lastUpdateTime, pm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                });
                break;
            case 6:
                cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).lastUpdateTime, pm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                };
                break;
            case 5:
                cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).firstInstallTime, pm.getPackageInfo(b.packageName, 0).firstInstallTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                });
                break;
            case 4:
                cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).firstInstallTime, pm.getPackageInfo(b.packageName, 0).firstInstallTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                };
                break;
            case 3:
                cmp = Collections.reverseOrder((a, b) -> a.packageName.compareTo(b.packageName));
                break;
            case 2:
                cmp = (a, b) -> a.packageName.compareTo(b.packageName);
                break;
            case 1:
                cmp = Collections.reverseOrder(displayNameComparator);
                break;
            case 0:
            default:
                cmp = displayNameComparator;
                break;
        }
        Collections.sort(fullList, (a, b) -> {
            if (XposedApp.getPreferences().getBoolean("enabled_top", true)) {
                boolean aChecked = checkedList.contains(a.packageName);
                boolean bChecked = checkedList.contains(b.packageName);
                if (aChecked == bChecked) {
                    return cmp.compare(a, b);
                } else if (aChecked) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return cmp.compare(a, b);
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo info = showList.get(position);
        holder.appIcon.setImageDrawable(info.loadIcon(pm));
        holder.appName.setText(Utils.getAppLabel(info, pm));
        try {
            holder.appVersion.setText(pm.getPackageInfo(info.packageName, 0).versionName);
            holder.appInstallTime.setText(dateformat.format(new Date(pm.getPackageInfo(info.packageName, 0).firstInstallTime)));
            holder.appUpdateTime.setText(dateformat.format(new Date(pm.getPackageInfo(info.packageName, 0).lastUpdateTime)));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.appPackage.setText(info.packageName);
        holder.appPackage.setTextColor(ThemeUtil.getThemeColor(context, android.R.attr.textColorSecondary));

        holder.mSwitch.setOnCheckedChangeListener(null);
        holder.mSwitch.setChecked(checkedList.contains(info.packageName));
        holder.mSwitch.setOnCheckedChangeListener((v, isChecked) ->
                onCheckedChange(v, isChecked, info));
        holder.infoLayout.setOnClickListener(v -> {
            if (callback != null) {
                callback.onItemClick(v, info);
            }
        });
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    public void filter(String constraint) {
        filter.filter(constraint);
    }

    public void refresh() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this::loadApps);
    }

    protected void onCheckedChange(CompoundButton buttonView, boolean isChecked, ApplicationInfo info) {
        // override this to implements your functions
    }

    public interface Callback {
        void onDataReady();

        void onItemClick(View v, ApplicationInfo info);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View infoLayout;
        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        TextView appVersion;
        TextView appInstallTime;
        TextView appUpdateTime;
        Switch mSwitch;

        ViewHolder(View itemView) {
            super(itemView);
            infoLayout = itemView.findViewById(R.id.info_layout);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appPackage = itemView.findViewById(R.id.package_name);
            appVersion = itemView.findViewById(R.id.version_name);
            appInstallTime = itemView.findViewById(R.id.tvInstallTime);
            appUpdateTime = itemView.findViewById(R.id.tvUpdateTime);
            mSwitch = itemView.findViewById(R.id.checkbox);
        }
    }

    class ApplicationFilter extends Filter {

        private boolean lowercaseContains(String s, CharSequence filter) {
            return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null || constraint.length() == 0) {
                showList = fullList;
            } else {
                showList = new ArrayList<>();
                String filter = constraint.toString().toLowerCase();
                for (ApplicationInfo info : fullList) {
                    if (lowercaseContains(Utils.getAppLabel(info, pm), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        showList.add(info);
                    }
                }
            }
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }
}
