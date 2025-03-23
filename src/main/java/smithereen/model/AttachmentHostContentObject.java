package smithereen.model;

import com.google.gson.JsonArray;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Audio;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.Video;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.AudioAttachment;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.attachments.VideoAttachment;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;
import spark.utils.StringUtils;

public sealed interface AttachmentHostContentObject permits MailMessage, PostLikeObject{
	List<ActivityPubObject> getAttachments();
	NonCachedRemoteImage.Args getPhotoArgs(int index);
	String getPhotoListID();

	default List<Attachment> getProcessedAttachments(){
		ArrayList<Attachment> result=new ArrayList<>();
		int i=0;
		for(ActivityPubObject o:getAttachments()){
			String mediaType=o.mediaType==null ? "" : o.mediaType;
			if(o instanceof Image || mediaType.startsWith("image/")){
				PhotoAttachment att=o instanceof Image img && img.isGraffiti ? new GraffitiAttachment() : new PhotoAttachment();
				if(StringUtils.isNotEmpty(o.name))
					att.description=o.name;
				if(o instanceof LocalImage li){
					att.image=li;
					att.photoID=li.photoID;
				}else{
					if(o.url==null)
						continue;
					// TODO make this less ugly
					MediaCache.PhotoItem item;
					try{
						item=(MediaCache.PhotoItem) MediaCache.getInstance().get(o.url);
					}catch(SQLException x){
						throw new InternalServerErrorException(x);
					}
					RemoteImage image;
					if(item!=null){
						image=new CachedRemoteImage(item, o.url);
					}else{
						SizedImage.Dimensions size=SizedImage.Dimensions.UNKNOWN;
						if(o instanceof Document im){
							if(im.width>0 && im.height>0){
								size=new SizedImage.Dimensions(im.width, im.height);
							}
						}
						image=new NonCachedRemoteImage(getPhotoArgs(i), size, o.url);
					}
					if(o instanceof Image img && img.photoApID!=null){
						image.photoActivityPubID=img.photoApID;
					}
					att.image=image;
				}
				if(o instanceof Document doc){
					if(StringUtils.isNotEmpty(doc.blurHash))
						att.blurHash=doc.blurHash;
				}
				result.add(att);
			}else if(o instanceof Video || mediaType.startsWith("video/")){
				if(o.url==null)
					continue;
				VideoAttachment att=new VideoAttachment();
				att.url=o.url;
				result.add(att);
			}else if(o instanceof Audio || mediaType.startsWith("audio/")){
				if(o.url==null)
					continue;
				AudioAttachment att=new AudioAttachment();
				att.description=o.name;
				att.url=o.url;
				result.add(att);
			}
			i++;
		}
		return result;
	}

	default String getSerializedAttachments(){
		List<ActivityPubObject> attachObjects=getAttachments();
		if(attachObjects!=null && !attachObjects.isEmpty()){
			if(attachObjects.size()==1){
				return MediaStorageUtils.serializeAttachment(attachObjects.getFirst()).toString();
			}else{
				JsonArray ar=new JsonArray();
				for(ActivityPubObject o:attachObjects){
					ar.add(MediaStorageUtils.serializeAttachment(o));
				}
				return ar.toString();
			}
		}
		return null;
	}

	@SuppressWarnings("unused") // Used from a template
	default boolean hasAudioAttachments(){
		for(ActivityPubObject o:getAttachments()){
			if(o instanceof Audio)
				return true;
		}
		return false;
	}
}
