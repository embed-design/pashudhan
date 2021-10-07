package com.embed.pashudhan.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.embed.pashudhan.Activities.OpenBlog
import com.embed.pashudhan.DataModels.BlogItem
import com.embed.pashudhan.R


class BlogsAdapter(
    private val mBlogsList: ArrayList<BlogItem>,
    private val mContext: Context
) :
    RecyclerView.Adapter<BlogsAdapter.MyViewHolder>() {

    companion object {
        private val TAG = "MyStoriesAdapter==>"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val itemview =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.samachar_card, parent, false)
        return MyViewHolder(itemview)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val blogItem: BlogItem = mBlogsList[position]

        holder.blogHeading.text = blogItem.title
        holder.blogExcerpt.text = blogItem.excerpt

        Glide.with(mContext).load(blogItem.thumbnail)
            .placeholder(R.drawable.download)
            .centerCrop()
            .into(holder.blogThumbnail)

        holder.blogView.setOnClickListener {
            val intent = Intent(mContext, OpenBlog::class.java)
            intent.putExtra("link", blogItem.link)
            mContext.startActivity(intent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return mBlogsList.size
    }

    class MyViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val blogThumbnail: ImageView = itemview.findViewById(R.id.blogImage)
        val blogHeading: TextView = itemview.findViewById(R.id.blogHeading)
        val blogExcerpt: TextView = itemview.findViewById(R.id.blogBody)
        val blogView: View = itemview
    }

}


