#
# Copyright (c) 2009--2010 Red Hat, Inc.
#
# This software is licensed to you under the GNU General Public License,
# version 2 (GPLv2). There is NO WARRANTY for this software, express or
# implied, including the implied warranties of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
# along with this software; if not, see
# http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
#
# Red Hat trademarks are not licensed under GPLv2. No permission is
# granted to use or replicate Red Hat trademarks that are incorporated
# in this software or its documentation.
#

import os
try:
    import hashlib
except ImportError:
    import md5
    import sha
    # pylint: disable=F0401
    # pylint can't find Crypto.Hash here, but it is present on older systems.
    from Crypto.Hash import SHA256 as sha256
    class hashlib:
        @staticmethod
        def new(checksum):
            if checksum == 'md5':
                return md5.new()
            elif checksum == 'sha1':
                return sha.new()
            elif checksum == 'sha256':
                return sha256.new()
            else:
                raise ValueError, "Incompatible checksum type"


def getFileChecksum(hashtype, filename=None, fd=None, file=None, buffer_size=None):
    """ Compute a file's checksum
        Used by rotateFile()
    """

    # python's md5 lib sucks
    # there's no way to directly import a file.
    if buffer_size is None:
        buffer_size = 65536

    if filename is None and fd is None and file is None:
        raise ValueError("no file specified")
    if file:
        f = file
    elif fd is not None:
        f = os.fdopen(os.dup(fd), "r")
    else:
        f = open(filename, "r")
    # Rewind it
    f.seek(0, 0)
    m = hashlib.new(hashtype)
    while 1:
        buffer = f.read(buffer_size)
        if not buffer:
            break
        m.update(buffer)

    # cleanup time
    if file is not None:
        file.seek(0, 0)
    else:
        f.close()
    return m.hexdigest()


def getStringChecksum(hashtype, s):
    """ compute checksum of an arbitrary string """
    ctx = hashlib.new(hashtype, s)
    return ctx.hexdigest()

def guess_checksum_type(checksum):
    """ guess the checksum type using the length """
    size = len(checksum)
    type = 'md5'
    if size == 32:
        type = 'md5'
    elif size == 40:
        type = 'sha1'
    elif size == 64:
        type = 'sha256'
    elif size == 96:
        type = 'sha384'
    elif size == 128:
        type = 'sha512'

    return type
