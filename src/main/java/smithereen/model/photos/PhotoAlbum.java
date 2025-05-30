package smithereen.model.photos;

import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;

import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.lang.Lang;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OwnedContentObject;
import smithereen.model.PrivacySetting;
import smithereen.model.User;
import smithereen.storage.DatabaseUtils;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class PhotoAlbum implements ActivityPubRepresentable, OwnedContentObject{
	public long id;
	public int ownerID;
	public String title;
	public String description;
	public PrivacySetting viewPrivacy, commentPrivacy;
	public int numPhotos;
	public Instant createdAt, updatedAt;
	public SystemAlbumType systemType;
	public long coverID;
	public EnumSet<Flag> flags;
	public int displayOrder;

	public URI activityPubID;
	public URI activityPubURL;
	public URI activityPubComments;

	public static PhotoAlbum fromResultSet(ResultSet res) throws SQLException{
		PhotoAlbum a=new PhotoAlbum();
		a.id=res.getLong("id");
		a.ownerID=res.getInt("owner_user_id");
		if(res.wasNull())
			a.ownerID=-res.getInt("owner_group_id");
		a.title=res.getString("title");
		a.description=res.getString("description");
		String privacy=res.getString("privacy");
		if(a.ownerID>0 && StringUtils.isNotEmpty(privacy)){
			Map<String, PrivacySetting> privacyMap=Utils.gson.fromJson(privacy, new TypeToken<>(){});
			a.viewPrivacy=privacyMap.get("view");
			a.commentPrivacy=privacyMap.get("comment");
		}
		a.numPhotos=res.getInt("num_photos");
		a.createdAt=DatabaseUtils.getInstant(res, "created_at");
		a.updatedAt=DatabaseUtils.getInstant(res, "updated_at");
		int sysType=res.getInt("system_type");
		if(!res.wasNull())
			a.systemType=SystemAlbumType.values()[sysType];
		a.coverID=res.getLong("cover_id");
		a.flags=EnumSet.noneOf(Flag.class);
		Utils.deserializeEnumSet(a.flags, Flag.class, res.getLong("flags"));
		String apID=res.getString("ap_id");
		String apURL=res.getString("ap_url");
		String apComments=res.getString("ap_comments");
		if(apID!=null && apURL!=null){
			a.activityPubID=URI.create(apID);
			a.activityPubURL=URI.create(apURL);
			if(apComments!=null)
				a.activityPubComments=URI.create(apComments);
		}
		a.displayOrder=res.getInt("display_order");
		return a;
	}

	public String getURL(){
		return "/albums/"+getIdString();
	}

	public String getIdString(){
		return Utils.encodeLong(XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.PHOTO_ALBUM));
	}

	public boolean hasFlag(String flag){
		return flags.contains(Flag.valueOf(flag));
	}

	@Override
	public URI getActivityPubID(){
		if(activityPubID!=null)
			return activityPubID;
		return UriBuilder.local().path("albums", getIdString()).build();
	}

	@Override
	public int getOwnerID(){
		return ownerID;
	}

	@Override
	public int getAuthorID(){
		return 0;
	}

	@Override
	public long getObjectID(){
		return id;
	}

	public String getLocalizedTitle(Lang lang, User self, Actor owner){
		return switch(systemType){
			case SAVED -> lang.get("saved_photos_album");
			case AVATARS -> {
				if(owner instanceof User user)
					yield self!=null && user.id==self.id ? lang.get("avatars_album_own") : lang.get("avatars_album_user", Map.of("name", user.getFirstAndGender()));
				else
					yield lang.get("avatars_album_group");
			}
			case TAGGED -> self!=null && self.id==owner.getOwnerID() ? lang.get("photos_of_me") : lang.get("photos_of_user", Map.of("name", ((User)owner).getFirstAndGender()));
			case null -> title;
		};
	}

	public enum SystemAlbumType{
		AVATARS,
		SAVED,
		TAGGED;

		public String getTitle(){
			return switch(this){
				case AVATARS -> "Profile pictures";
				case SAVED -> "Saved photos";
				case TAGGED -> throw new IllegalStateException("Tagged photos is not a real album");
			};
		}
	}

	public enum Flag{
		COVER_SET_EXPLICITLY,
		GROUP_DISABLE_COMMENTING,
		GROUP_RESTRICT_UPLOADS
	}
}
