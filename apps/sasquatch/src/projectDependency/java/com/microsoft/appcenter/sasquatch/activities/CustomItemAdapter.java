/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;

import java.util.ArrayList;

public class CustomItemAdapter extends RecyclerView.Adapter<CustomItemAdapter.CustomItemAdapterHolder> {

    private ArrayList<String> mList;
    private Context mContext;
    private AppDocumentListAdapter.OnItemClickListener listener;

    CustomItemAdapter(ArrayList<String> list, Context context) {
        mList = list;
        mContext = context;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public CustomItemAdapterHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new CustomItemAdapterHolder(LayoutInflater.from(mContext).inflate(R.layout.item_view_property, null, false));
    }

    void setOnItemClickListener(AppDocumentListAdapter.OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull CustomItemAdapterHolder holder, @SuppressLint("RecyclerView") final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(position);
                }
            }
        });
        holder.listItemText.setText(mList.get(position));
        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Storage.delete(Constants.USER, StorageActivity.sUserDocumentList.get(position));
                mList.remove(position);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class CustomItemAdapterHolder extends RecyclerView.ViewHolder {

        TextView listItemText;
        ImageButton deleteBtn;

        CustomItemAdapterHolder(@NonNull View itemView) {
            super(itemView);
            listItemText = itemView.findViewById(R.id.property);
            deleteBtn = itemView.findViewById(R.id.delete_button);
        }
    }
}
