package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;

public class Relationship extends ActivityPubObject{

	public static final URI FRIEND_OF=URI.create("http://purl.org/vocab/relationship/friendOf");

	public LinkOrObject subject;
	public LinkOrObject object;
	public URI relationship;

	@Override
	public String getType(){
		return "Relationship";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		obj.addProperty("relationship", relationship.toString());
		obj.add("object", object.serialize(serializerContext));
		obj.add("subject", subject.serialize(serializerContext));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		relationship=tryParseURL(obj.get("relationship").getAsString());
		object=tryParseLinkOrObject(obj.get("object"), parserContext);
		subject=tryParseLinkOrObject(obj.get("subject"), parserContext);
		return this;
	}
}
