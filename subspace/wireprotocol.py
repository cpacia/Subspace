# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: wireprotocol.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf.internal import enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='wireprotocol.proto',
  package='',
  serialized_pb=_b('\n\x12wireprotocol.proto\"p\n\x07Message\x12\r\n\x05magic\x18\x01 \x02(\x0c\x12\x11\n\tmessageID\x18\x02 \x02(\x0c\x12\x15\n\x06sender\x18\x03 \x02(\x0b\x32\x05.Node\x12\x19\n\x07\x63ommand\x18\x04 \x02(\x0e\x32\x08.Command\x12\x11\n\targuments\x18\x05 \x03(\t\"3\n\x04Node\x12\x0e\n\x06nodeID\x18\x03 \x02(\t\x12\x1b\n\x08services\x18\x04 \x03(\x0e\x32\t.Services*U\n\x07\x43ommand\x12\x08\n\x04PING\x10\x01\x12\t\n\x05STORE\x10\x02\x12\x07\n\x03RTC\x10\x03\x12\r\n\tFIND_NODE\x10\x04\x12\x0e\n\nFIND_VALUE\x10\x05\x12\r\n\tGET_NODES\x10\x06**\n\x08Services\x12\r\n\tNODE_LITE\x10\x01\x12\x0f\n\x0bNODE_SERVER\x10\x02')
)
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

_COMMAND = _descriptor.EnumDescriptor(
  name='Command',
  full_name='Command',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='PING', index=0, number=1,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='STORE', index=1, number=2,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='RTC', index=2, number=3,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='FIND_NODE', index=3, number=4,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='FIND_VALUE', index=4, number=5,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GET_NODES', index=5, number=6,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=189,
  serialized_end=274,
)
_sym_db.RegisterEnumDescriptor(_COMMAND)

Command = enum_type_wrapper.EnumTypeWrapper(_COMMAND)
_SERVICES = _descriptor.EnumDescriptor(
  name='Services',
  full_name='Services',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='NODE_LITE', index=0, number=1,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='NODE_SERVER', index=1, number=2,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=276,
  serialized_end=318,
)
_sym_db.RegisterEnumDescriptor(_SERVICES)

Services = enum_type_wrapper.EnumTypeWrapper(_SERVICES)
PING = 1
STORE = 2
RTC = 3
FIND_NODE = 4
FIND_VALUE = 5
GET_NODES = 6
NODE_LITE = 1
NODE_SERVER = 2



_MESSAGE = _descriptor.Descriptor(
  name='Message',
  full_name='Message',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='magic', full_name='Message.magic', index=0,
      number=1, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value=_b(""),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='messageID', full_name='Message.messageID', index=1,
      number=2, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value=_b(""),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='sender', full_name='Message.sender', index=2,
      number=3, type=11, cpp_type=10, label=2,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='command', full_name='Message.command', index=3,
      number=4, type=14, cpp_type=8, label=2,
      has_default_value=False, default_value=1,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='arguments', full_name='Message.arguments', index=4,
      number=5, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=22,
  serialized_end=134,
)


_NODE = _descriptor.Descriptor(
  name='Node',
  full_name='Node',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='nodeID', full_name='Node.nodeID', index=0,
      number=3, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='services', full_name='Node.services', index=1,
      number=4, type=14, cpp_type=8, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=136,
  serialized_end=187,
)

_MESSAGE.fields_by_name['sender'].message_type = _NODE
_MESSAGE.fields_by_name['command'].enum_type = _COMMAND
_NODE.fields_by_name['services'].enum_type = _SERVICES
DESCRIPTOR.message_types_by_name['Message'] = _MESSAGE
DESCRIPTOR.message_types_by_name['Node'] = _NODE
DESCRIPTOR.enum_types_by_name['Command'] = _COMMAND
DESCRIPTOR.enum_types_by_name['Services'] = _SERVICES

Message = _reflection.GeneratedProtocolMessageType('Message', (_message.Message,), dict(
  DESCRIPTOR = _MESSAGE,
  __module__ = 'wireprotocol_pb2'
  # @@protoc_insertion_point(class_scope:Message)
  ))
_sym_db.RegisterMessage(Message)

Node = _reflection.GeneratedProtocolMessageType('Node', (_message.Message,), dict(
  DESCRIPTOR = _NODE,
  __module__ = 'wireprotocol_pb2'
  # @@protoc_insertion_point(class_scope:Node)
  ))
_sym_db.RegisterMessage(Node)


# @@protoc_insertion_point(module_scope)
